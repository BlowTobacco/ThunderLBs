package exposed.thunder.thunderLBs.placeholder;

import exposed.thunder.thunderLBs.ThunderLBs;
import exposed.thunder.thunderLBs.leaderboard.LeaderboardType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

final class TopperQueryBridge implements NativeLeaderboardProvider {
    private static final String TOP_TEMPLATE_CLASS =
            "me.hsgamer.topper.spigot.plugin.template.SpigotTopTemplate";
    private static final Set<String> SUPPORTED_CONTEXTS =
            Set.of("topper", "grouptopper", "timedtopper");

    private final ThunderLBs plugin;
    private final Map<String, QueryContext> contexts = new ConcurrentHashMap<>();
    private final Consumer<Object> forwarder = this::acceptContext;
    private Object topTemplate;
    private Object queryForwardManager;
    private Method removeForwarder;
    private boolean available;

    TopperQueryBridge(ThunderLBs plugin) {
        this.plugin = plugin;
        attach();
    }

    @Override
    public boolean supports(String providerName, LeaderboardType leaderboardType) {
        return contextName(providerName, leaderboardType) != null;
    }

    @Override
    public String resolve(Request request) throws ReflectiveOperationException {
        String contextName = contextName(request.providerName(), request.leaderboardType());
        if (!available || contextName == null) {
            return null;
        }
        QueryContext context = contexts.get(contextName);
        if (context == null) {
            return null;
        }

        String query = queryString(request);
        if (query == null) {
            return null;
        }
        UUID playerId = request.player() == null ? null : request.player().getUniqueId();
        Object queryFunction = context.queryMethod().invoke(context.context());
        if (!(queryFunction instanceof BiFunction<?, ?, ?> rawFunction)) {
            throw new ReflectiveOperationException("Topper query context returned a non-BiFunction query");
        }

        @SuppressWarnings("unchecked")
        BiFunction<UUID, String, Object> function = (BiFunction<UUID, String, Object>) rawFunction;
        Object result;
        try {
            result = function.apply(playerId, query);
        } catch (RuntimeException exception) {
            throw new InvocationTargetException(exception);
        }
        return readResult(result);
    }

    @Override
    public List<String> holderChoices(String providerName, LeaderboardType leaderboardType)
            throws ReflectiveOperationException {
        String contextName = contextName(providerName, leaderboardType);
        if (contextName == null) {
            return List.of();
        }
        Object manager = switch (contextName) {
            case "topper" -> manager(topTemplate, "getTopManager");
            case "grouptopper" -> pluginManager("GroupTopper", "getGroupTopManager");
            case "timedtopper" -> pluginManager("TimedTopper", "getTopManager");
            default -> null;
        };
        if (manager == null) {
            return List.of();
        }
        Object names = manager.getClass().getMethod("getHolderNames").invoke(manager);
        if (!(names instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String name() {
        return "Topper";
    }

    @Override
    public void close() {
        available = false;
        contexts.clear();
        if (queryForwardManager == null || removeForwarder == null) {
            return;
        }
        try {
            removeForwarder.invoke(queryForwardManager, forwarder);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("Could not detach the native Topper query bridge: "
                    + rootMessage(exception));
        } finally {
            topTemplate = null;
            queryForwardManager = null;
            removeForwarder = null;
        }
    }

    private void attach() {
        Plugin topper = plugin.getServer().getPluginManager().getPlugin("Topper");
        if (topper == null || !topper.isEnabled()) {
            return;
        }

        try {
            ClassLoader classLoader = topper.getClass().getClassLoader();
            Class<?> templateClass = Class.forName(TOP_TEMPLATE_CLASS, false, classLoader);
            Method getComponent = topper.getClass().getMethod("get", Class.class);
            Object template = getComponent.invoke(topper, templateClass);
            if (template == null) {
                throw new ReflectiveOperationException("Topper did not expose SpigotTopTemplate");
            }
            this.topTemplate = template;

            Method getManager = templateClass.getMethod("getQueryForwardManager");
            Object manager = getManager.invoke(template);
            Method addForwarder = findConsumerMethod(manager.getClass(), "addForwarder");
            this.removeForwarder = findConsumerMethod(manager.getClass(), "removeForwarder");
            this.queryForwardManager = manager;
            addForwarder.invoke(manager, forwarder);
            this.available = true;
            plugin.getLogger().info("Hooked Topper's native query registry; PlaceholderAPI is not used for "
                    + "Topper, GroupTopper, or TimedTopper queries.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (queryForwardManager != null && removeForwarder != null) {
                try {
                    removeForwarder.invoke(queryForwardManager, forwarder);
                } catch (ReflectiveOperationException ignored) {}
            }
            contexts.clear();
            topTemplate = null;
            queryForwardManager = null;
            removeForwarder = null;
            plugin.getLogger().warning("Topper is installed, but its native query registry could not be hooked: "
                    + rootMessage(exception));
        }
    }

    private Object pluginManager(String pluginName, String getter) throws ReflectiveOperationException {
        Plugin target = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (target == null || !target.isEnabled()) {
            return null;
        }
        return manager(target, getter);
    }

    private static Object manager(Object owner, String getter) throws ReflectiveOperationException {
        return owner == null ? null : owner.getClass().getMethod(getter).invoke(owner);
    }

    private void acceptContext(Object context) {
        if (context == null) {
            return;
        }
        try {
            Method getName = context.getClass().getMethod("getName");
            getName.trySetAccessible();
            String name = String.valueOf(getName.invoke(context)).toLowerCase(Locale.ROOT);
            if (!SUPPORTED_CONTEXTS.contains(name)) {
                return;
            }
            Method getQuery = context.getClass().getMethod("getQuery");
            getQuery.trySetAccessible();
            contexts.put(name, new QueryContext(context, getQuery));
            plugin.getLogger().info("Registered native Topper query context '" + name + "'.");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Ignored an incompatible Topper query context: " + rootMessage(exception));
        }
    }

    private static Method findConsumerMethod(Class<?> type, String name) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)
                    && method.getParameterCount() == 1
                    && Consumer.class.isAssignableFrom(method.getParameterTypes()[0])) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name + "(Consumer)");
    }

    private static String readResult(Object result) throws ReflectiveOperationException {
        if (result == null) {
            return null;
        }
        Class<?> type = result.getClass();
        try {
            Field field = type.getField("result");
            Object value = field.get(result);
            return value == null ? null : value.toString();
        } catch (NoSuchFieldException ignored) {}
        for (String methodName : new String[] { "result", "getResult" }) {
            try {
                Object value = type.getMethod(methodName).invoke(result);
                return value == null ? null : value.toString();
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchFieldException(type.getName() + "#result");
    }

    static String contextName(String providerName, LeaderboardType leaderboardType) {
        if (providerName == null || leaderboardType == null) {
            return null;
        }
        if (providerName.equalsIgnoreCase("topper")) {
            return leaderboardType == LeaderboardType.GROUP ? "grouptopper" : "topper";
        }
        if (providerName.equalsIgnoreCase("timedtopper") && leaderboardType == LeaderboardType.PLAYER) {
            return "timedtopper";
        }
        return null;
    }

    static String queryString(Request request) {
        if (request == null || request.holder() == null || request.holder().isBlank()) {
            return null;
        }
        return switch (request.value()) {
            case TOP_NAME -> request.position() < 1
                    ? null : request.holder() + ";top_name;" + request.position();
            case TOP_VALUE -> request.position() < 1
                    ? null : request.holder() + ";top_value_raw;" + request.position();
            case VIEWER_RANK -> request.holder() + ";top_rank";
            case VIEWER_VALUE -> request.holder() + ";value_raw";
        };
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private record QueryContext(Object context, Method queryMethod) {
    }
}
