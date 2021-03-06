package org.bukkit.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerEventException;
import com.destroystokyo.paper.exception.ServerPluginEnableDisableException;
import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.FileUtil;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashObjSets;

/**
 * Handles all plugin management from the Server
 */
public final class SimplePluginManager implements PluginManager {
    private final Server server;
    private final Map<Pattern, PluginLoader> fileAssociations = HashObjObjMaps.newMutableMap(); // Akarin
    private final List<Plugin> plugins = new ArrayList<Plugin>();
    private final Map<String, Plugin> lookupNames = HashObjObjMaps.newMutableMap(); // Akarin
    private File updateDirectory;
    private final SimpleCommandMap commandMap;
    private Map<String, Permission> permissions = Collections.emptyMap(); // Akarin
    private final Object permissionsLock = new Object();
    private Map<Boolean, Set<Permission>> defaultPerms; // Akarin
    private final Map<String, Map<Permissible, Boolean>> permSubs = HashObjObjMaps.newMutableMap(); // Akarin
    private final Object permSubsLock = new Object();
    private final Map<Boolean, Map<Permissible, Boolean>> defSubs = HashObjObjMaps.newMutableMap(); // Akarin
    private boolean useTimings = false;

    public SimplePluginManager(@Nonnull Server instance, @Nonnull SimpleCommandMap commandMap) { // Akarin - javax.annotation
        server = instance;
        this.commandMap = commandMap;

        // Akarin start
        HashObjObjMap<Boolean, Set<Permission>> defaultPerms = HashObjObjMaps.newUpdatableMap();
        defaultPerms.put(Boolean.TRUE, HashObjSets.newUpdatableSet());
        defaultPerms.put(Boolean.FALSE, HashObjSets.newUpdatableSet());
        this.defaultPerms = defaultPerms;
        // Akarin end
    }

    /**
     * Registers the specified plugin loader
     *
     * @param loader Class name of the PluginLoader to register
     * @throws IllegalArgumentException Thrown when the given Class is not a
     *     valid PluginLoader
     */
    public void registerInterface(@Nonnull Class<? extends PluginLoader> loader) throws IllegalArgumentException { // Akarin - javax.annotation
        PluginLoader instance;

        if (PluginLoader.class.isAssignableFrom(loader)) {
            Constructor<? extends PluginLoader> constructor;

            try {
                constructor = loader.getConstructor(Server.class);
                instance = constructor.newInstance(server);
            } catch (NoSuchMethodException ex) {
                String className = loader.getName();

                throw new IllegalArgumentException(String.format("Class %s does not have a public %s(Server) constructor", className, className), ex);
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format("Unexpected exception %s while attempting to construct a new instance of %s", ex.getClass().getName(), loader.getName()), ex);
            }
        } else {
            throw new IllegalArgumentException(String.format("Class %s does not implement interface PluginLoader", loader.getName()));
        }

        Pattern[] patterns = instance.getPluginFileFilters();

        synchronized (this) {
            for (Pattern pattern : patterns) {
                fileAssociations.put(pattern, instance);
            }
        }
    }

    /**
     * Loads the plugins contained within the specified directory
     *
     * @param directory Directory to check for plugins
     * @return A list of all plugins loaded
     */
    @Nonnull // Akarin - javax.annotation
    public Plugin[] loadPlugins(@Nonnull File directory) { // Akarin - javax.annotation
        Validate.notNull(directory, "Directory cannot be null");
        Validate.isTrue(directory.isDirectory(), "Directory must be a directory");

        List<Plugin> result = new ArrayList<Plugin>();
        Set<Pattern> filters = fileAssociations.keySet();

        if (!(server.getUpdateFolder().equals(""))) {
            updateDirectory = new File(directory, server.getUpdateFolder());
        }

        Map<String, File> plugins = new HashMap<String, File>();
        Set<String> loadedPlugins = new HashSet<String>();
        Map<String, Collection<String>> dependencies = new HashMap<String, Collection<String>>();
        Map<String, Collection<String>> softDependencies = new HashMap<String, Collection<String>>();

        // This is where it figures out all possible plugins
        for (File file : directory.listFiles()) {
            PluginLoader loader = null;
            for (Pattern filter : filters) {
                Matcher match = filter.matcher(file.getName());
                if (match.find()) {
                    loader = fileAssociations.get(filter);
                }
            }

            if (loader == null) continue;

            PluginDescriptionFile description = null;
            try {
                description = loader.getPluginDescription(file);
                String name = description.getName();
                if (name.equalsIgnoreCase("bukkit") || name.equalsIgnoreCase("minecraft") || name.equalsIgnoreCase("mojang")) {
                    server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "': Restricted Name");
                    continue;
                } else if (description.rawName.indexOf(' ') != -1) {
                    server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "': uses the space-character (0x20) in its name");
                    continue;
                }
            } catch (InvalidDescriptionException ex) {
                server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex);
                continue;
            }

            File replacedFile = plugins.put(description.getName(), file);
            if (replacedFile != null) {
                server.getLogger().severe(String.format(
                    "Ambiguous plugin name `%s' for files `%s' and `%s' in `%s'",
                    description.getName(),
                    file.getPath(),
                    replacedFile.getPath(),
                    directory.getPath()
                ));
            }

            Collection<String> softDependencySet = description.getSoftDepend();
            if (softDependencySet != null && !softDependencySet.isEmpty()) {
                if (softDependencies.containsKey(description.getName())) {
                    // Duplicates do not matter, they will be removed together if applicable
                    softDependencies.get(description.getName()).addAll(softDependencySet);
                } else {
                    softDependencies.put(description.getName(), new LinkedList<String>(softDependencySet));
                }
            }

            Collection<String> dependencySet = description.getDepend();
            if (dependencySet != null && !dependencySet.isEmpty()) {
                dependencies.put(description.getName(), new LinkedList<String>(dependencySet));
            }

            Collection<String> loadBeforeSet = description.getLoadBefore();
            if (loadBeforeSet != null && !loadBeforeSet.isEmpty()) {
                for (String loadBeforeTarget : loadBeforeSet) {
                    if (softDependencies.containsKey(loadBeforeTarget)) {
                        softDependencies.get(loadBeforeTarget).add(description.getName());
                    } else {
                        // softDependencies is never iterated, so 'ghost' plugins aren't an issue
                        Collection<String> shortSoftDependency = new LinkedList<String>();
                        shortSoftDependency.add(description.getName());
                        softDependencies.put(loadBeforeTarget, shortSoftDependency);
                    }
                }
            }
        }

        while (!plugins.isEmpty()) {
            boolean missingDependency = true;
            Iterator<Map.Entry<String, File>> pluginIterator = plugins.entrySet().iterator();

            while (pluginIterator.hasNext()) {
                Map.Entry<String, File> entry = pluginIterator.next();
                String plugin = entry.getKey();

                if (dependencies.containsKey(plugin)) {
                    Iterator<String> dependencyIterator = dependencies.get(plugin).iterator();

                    while (dependencyIterator.hasNext()) {
                        String dependency = dependencyIterator.next();

                        // Dependency loaded
                        if (loadedPlugins.contains(dependency)) {
                            dependencyIterator.remove();

                        // We have a dependency not found
                        } else if (!plugins.containsKey(dependency)) {
                            missingDependency = false;
                            pluginIterator.remove();
                            softDependencies.remove(plugin);
                            dependencies.remove(plugin);

                            server.getLogger().log(
                                Level.SEVERE,
                                "Could not load '" + entry.getValue().getPath() + "' in folder '" + directory.getPath() + "'",
                                new UnknownDependencyException(dependency));
                            break;
                        }
                    }

                    if (dependencies.containsKey(plugin) && dependencies.get(plugin).isEmpty()) {
                        dependencies.remove(plugin);
                    }
                }
                if (softDependencies.containsKey(plugin)) {
                    Iterator<String> softDependencyIterator = softDependencies.get(plugin).iterator();

                    while (softDependencyIterator.hasNext()) {
                        String softDependency = softDependencyIterator.next();

                        // Soft depend is no longer around
                        if (!plugins.containsKey(softDependency)) {
                            softDependencyIterator.remove();
                        }
                    }

                    if (softDependencies.get(plugin).isEmpty()) {
                        softDependencies.remove(plugin);
                    }
                }
                if (!(dependencies.containsKey(plugin) || softDependencies.containsKey(plugin)) && plugins.containsKey(plugin)) {
                    // We're clear to load, no more soft or hard dependencies left
                    File file = plugins.get(plugin);
                    pluginIterator.remove();
                    missingDependency = false;

                    try {
                        result.add(loadPlugin(file));
                        loadedPlugins.add(plugin);
                        continue;
                    } catch (InvalidPluginException ex) {
                        server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex);
                    }
                }
            }

            if (missingDependency) {
                // We now iterate over plugins until something loads
                // This loop will ignore soft dependencies
                pluginIterator = plugins.entrySet().iterator();

                while (pluginIterator.hasNext()) {
                    Map.Entry<String, File> entry = pluginIterator.next();
                    String plugin = entry.getKey();

                    if (!dependencies.containsKey(plugin)) {
                        softDependencies.remove(plugin);
                        missingDependency = false;
                        File file = entry.getValue();
                        pluginIterator.remove();

                        try {
                            result.add(loadPlugin(file));
                            loadedPlugins.add(plugin);
                            break;
                        } catch (InvalidPluginException ex) {
                            server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex);
                        }
                    }
                }
                // We have no plugins left without a depend
                if (missingDependency) {
                    softDependencies.clear();
                    dependencies.clear();
                    Iterator<File> failedPluginIterator = plugins.values().iterator();

                    while (failedPluginIterator.hasNext()) {
                        File file = failedPluginIterator.next();
                        failedPluginIterator.remove();
                        server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "': circular dependency detected");
                    }
                }
            }
        }

        return result.toArray(new Plugin[result.size()]);
    }

    /**
     * Loads the plugin in the specified file
     * <p>
     * File must be valid according to the current enabled Plugin interfaces
     *
     * @param file File containing the plugin to load
     * @return The Plugin loaded, or null if it was invalid
     * @throws InvalidPluginException Thrown when the specified file is not a
     *     valid plugin
     * @throws UnknownDependencyException If a required dependency could not
     *     be found
     */
    @Nullable
    public synchronized Plugin loadPlugin(@Nonnull File file) throws InvalidPluginException, UnknownDependencyException { // Akarin - javax.annotation
        Validate.notNull(file, "File cannot be null");

        checkUpdate(file);

        Set<Pattern> filters = fileAssociations.keySet();
        Plugin result = null;

        for (Pattern filter : filters) {
            String name = file.getName();
            Matcher match = filter.matcher(name);

            if (match.find()) {
                PluginLoader loader = fileAssociations.get(filter);

                result = loader.loadPlugin(file);
            }
        }

        if (result != null) {
            plugins.add(result);
            lookupNames.put(result.getDescription().getName().toLowerCase(java.util.Locale.ENGLISH), result); // Paper
        }

        return result;
    }

    private void checkUpdate(@Nonnull File file) { // Akarin - javax.annotation
        if (updateDirectory == null || !updateDirectory.isDirectory()) {
            return;
        }

        File updateFile = new File(updateDirectory, file.getName());
        if (updateFile.isFile() && FileUtil.copy(updateFile, file)) {
            updateFile.delete();
        }
    }

    /**
     * Checks if the given plugin is loaded and returns it when applicable
     * <p>
     * Please note that the name of the plugin is case-sensitive
     *
     * @param name Name of the plugin to check
     * @return Plugin if it exists, otherwise null
     */
    @Nullable
    public synchronized Plugin getPlugin(@Nonnull String name) { // Akarin - javax.annotation
        return lookupNames.get(name.replace(' ', '_').toLowerCase(java.util.Locale.ENGLISH)); // Paper
    }

    @Nonnull // Akarin - javax.annotation
    public synchronized Plugin[] getPlugins() {
        return plugins.toArray(new Plugin[plugins.size()]);
    }

    /**
     * Checks if the given plugin is enabled or not
     * <p>
     * Please note that the name of the plugin is case-sensitive.
     *
     * @param name Name of the plugin to check
     * @return true if the plugin is enabled, otherwise false
     */
    public boolean isPluginEnabled(@Nonnull String name) { // Akarin - javax.annotation
        Plugin plugin = getPlugin(name);

        return isPluginEnabled(plugin);
    }

    /**
     * Checks if the given plugin is enabled or not
     *
     * @param plugin Plugin to check
     * @return true if the plugin is enabled, otherwise false
     */
    public synchronized boolean isPluginEnabled(@Nullable Plugin plugin) { // Paper - synchronize
        if ((plugin != null) && (plugins.contains(plugin))) {
            return plugin.isEnabled();
        } else {
            return false;
        }
    }

    public synchronized void enablePlugin(@Nonnull final Plugin plugin) { // Paper - synchronize // Akarin - javax.annotation
        if (!plugin.isEnabled()) {
            List<Command> pluginCommands = PluginCommandYamlParser.parse(plugin);

            if (!pluginCommands.isEmpty()) {
                commandMap.registerAll(plugin.getDescription().getName(), pluginCommands);
            }

            try {
                plugin.getPluginLoader().enablePlugin(plugin);
            } catch (Throwable ex) {
                handlePluginException("Error occurred (in the plugin loader) while enabling "
                        + plugin.getDescription().getFullName() + " (Is it up to date?)", ex, plugin);
            }

            HandlerList.bakeAll();
        }
    }

    // Paper start - close Classloader on disable
    public void disablePlugins() {
        disablePlugins(false);
    }

    public void disablePlugins(boolean closeClassloaders) {
        // Paper end - close Classloader on disable
        Plugin[] plugins = getPlugins();
        for (int i = plugins.length - 1; i >= 0; i--) {
            disablePlugin(plugins[i], closeClassloaders); // Paper - close Classloader on disable
        }
    }

    // Paper start - close Classloader on disable
    public void disablePlugin(@Nonnull final Plugin plugin) { // Akarin - javax.annotation
        disablePlugin(plugin, false);
    }

    public synchronized void disablePlugin(@Nonnull final Plugin plugin, boolean closeClassloader) { // Paper - synchronize // Akarin - javax.annotation
        // Paper end - close Classloader on disable
        if (plugin.isEnabled()) {
            try {
                plugin.getPluginLoader().disablePlugin(plugin, closeClassloader); // Paper - close Classloader on disable
            } catch (Throwable ex) {
                handlePluginException("Error occurred (in the plugin loader) while disabling "
                        + plugin.getDescription().getFullName() + " (Is it up to date?)", ex, plugin); // Paper
            }

            try {
                server.getScheduler().cancelTasks(plugin);
            } catch (Throwable ex) {
                handlePluginException("Error occurred (in the plugin loader) while cancelling tasks for "
                        + plugin.getDescription().getFullName() + " (Is it up to date?)", ex, plugin); // Paper
            }

            try {
                server.getServicesManager().unregisterAll(plugin);
            } catch (Throwable ex) {
                handlePluginException("Error occurred (in the plugin loader) while unregistering services for "
                        + plugin.getDescription().getFullName() + " (Is it up to date?)", ex, plugin); // Paper
            }

            try {
                HandlerList.unregisterAll(plugin);
            } catch (Throwable ex) {
                handlePluginException("Error occurred (in the plugin loader) while unregistering events for "
                        + plugin.getDescription().getFullName() + " (Is it up to date?)", ex, plugin); // Paper
            }

            try {
                server.getMessenger().unregisterIncomingPluginChannel(plugin);
                server.getMessenger().unregisterOutgoingPluginChannel(plugin);
            } catch (Throwable ex) {
                handlePluginException("Error occurred (in the plugin loader) while unregistering plugin channels for "
                        + plugin.getDescription().getFullName() + " (Is it up to date?)", ex, plugin); // Paper
            }
        }
    }

    // Paper start
    private void handlePluginException(String msg, Throwable ex, Plugin plugin) {
        server.getLogger().log(Level.SEVERE, msg, ex);
        callEvent(new ServerExceptionEvent(new ServerPluginEnableDisableException(msg, ex, plugin)));
    }
    // Paper end

    public void clearPlugins() {
        synchronized (this) {
            disablePlugins(true); // Paper - close Classloader on disable
            plugins.clear();
            lookupNames.clear();
            HandlerList.unregisterAll();
            fileAssociations.clear();
            synchronized (permissionsLock) { permissions = Collections.emptyMap(); } // Akarin 
            // Akarin start
            //defaultPerms.get(true).clear();
            //defaultPerms.get(false).clear();
            HashObjObjMap<Boolean, Set<Permission>> defaultPerms = HashObjObjMaps.newUpdatableMap();
            defaultPerms.put(Boolean.TRUE, HashObjSets.newUpdatableSet());
            defaultPerms.put(Boolean.FALSE, HashObjSets.newUpdatableSet());
            this.defaultPerms = defaultPerms;
            // Akarin end
        }
    }
    private void fireEvent(Event event) { callEvent(event); } // Paper - support old method incase plugin uses reflection

    /**
     * Calls an event with the given details.
     * <p>
     * This method only synchronizes when the event is not asynchronous.
     *
     * @param event Event details
     */
    public void callEvent(@Nonnull Event event) { // Akarin - javax.annotation
        // Paper - replace callEvent by merging to below method
        HandlerList handlers = event.getHandlers();
        RegisteredListener[] listeners = handlers.getRegisteredListeners();

        for (RegisteredListener registration : listeners) {
            if (!registration.getPlugin().isEnabled()) {
                continue;
            }

            try {
                registration.callEvent(event);
            } catch (AuthorNagException ex) {
                Plugin plugin = registration.getPlugin();

                if (plugin.isNaggable()) {
                    plugin.setNaggable(false);

                    server.getLogger().log(Level.SEVERE, String.format(
                            "Nag author(s): '%s' of '%s' about the following: %s",
                            plugin.getDescription().getAuthors(),
                            plugin.getDescription().getFullName(),
                            ex.getMessage()
                            ));
                }
            } catch (Throwable ex) {
                // Paper start - error reporting
                String msg = "Could not pass event " + event.getEventName() + " to " + registration.getPlugin().getDescription().getFullName();
                server.getLogger().log(Level.SEVERE, msg, ex);
                if (!(event instanceof ServerExceptionEvent)) { // We don't want to cause an endless event loop
                    callEvent(new ServerExceptionEvent(new ServerEventException(msg, ex, registration.getPlugin(), registration.getListener(), event)));
                }
                // Paper end
            }
        }
    }

    public void registerEvents(@Nonnull Listener listener, @Nonnull Plugin plugin) { // Akarin - javax.annotation
        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register " + listener + " while not enabled");
        }

        for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : plugin.getPluginLoader().createRegisteredListeners(listener, plugin).entrySet()) {
            getEventListeners(getRegistrationClass(entry.getKey())).registerAll(entry.getValue());
        }

    }

    public void registerEvent(@Nonnull Class<? extends Event> event, @Nonnull Listener listener, @Nonnull EventPriority priority, @Nonnull EventExecutor executor, @Nonnull Plugin plugin) { // Akarin - javax.annotation
        registerEvent(event, listener, priority, executor, plugin, false);
    }

    /**
     * Registers the given event to the specified listener using a directly
     * passed EventExecutor
     *
     * @param event Event class to register
     * @param listener PlayerListener to register
     * @param priority Priority of this event
     * @param executor EventExecutor to register
     * @param plugin Plugin to register
     * @param ignoreCancelled Do not call executor if event was already
     *     cancelled
     */
    public void registerEvent(@Nonnull Class<? extends Event> event, @Nonnull Listener listener, @Nonnull EventPriority priority, @Nonnull EventExecutor executor, @Nonnull Plugin plugin, boolean ignoreCancelled) { // Akarin - javax.annotation
        Validate.notNull(listener, "Listener cannot be null");
        Validate.notNull(priority, "Priority cannot be null");
        Validate.notNull(executor, "Executor cannot be null");
        Validate.notNull(plugin, "Plugin cannot be null");

        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register " + event + " while not enabled");
        }

        executor = new co.aikar.timings.TimedEventExecutor(executor, plugin, null, event); // Paper
        if (false) { // Spigot - RL handles useTimings check now // Paper
            getEventListeners(event).register(new TimedRegisteredListener(listener, executor, priority, plugin, ignoreCancelled));
        } else {
            getEventListeners(event).register(new RegisteredListener(listener, executor, priority, plugin, ignoreCancelled));
        }
    }

    @Nonnull // Akarin - javax.annotation
    private HandlerList getEventListeners(@Nonnull Class<? extends Event> type) { // Akarin - javax.annotation
        try {
            Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            return (HandlerList) method.invoke(null);
        } catch (Exception e) {
            throw new IllegalPluginAccessException(e.toString());
        }
    }

    @Nonnull // Akarin - javax.annotation
    private Class<? extends Event> getRegistrationClass(@Nonnull Class<? extends Event> clazz) { // Akarin - javax.annotation
        try {
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null
                    && !clazz.getSuperclass().equals(Event.class)
                    && Event.class.isAssignableFrom(clazz.getSuperclass())) {
                return getRegistrationClass(clazz.getSuperclass().asSubclass(Event.class));
            } else {
                throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName() + ". Static getHandlerList method required!");
            }
        }
    }

    @Nullable
    public Permission getPermission(@Nonnull String name) { // Akarin - javax.annotation
        return permissions.get(name.toLowerCase(java.util.Locale.ENGLISH));
    }

    public void addPermission(@Nonnull Permission perm) { // Akarin - javax.annotation
        addPermission(perm, true);
    }

    @Deprecated
    public void addPermission(@Nonnull Permission perm, boolean dirty) { // Akarin - javax.annotation
        String name = perm.getName().toLowerCase(java.util.Locale.ENGLISH);

        if (permissions.containsKey(name)) {
            throw new IllegalArgumentException("The permission " + name + " is already defined!");
        }

        // Akarin start
        synchronized (permissionsLock) {
            HashObjObjMap<String, Permission> toImmutable = HashObjObjMaps.newUpdatableMap(permissions);
            toImmutable.put(name, perm);
            permissions = HashObjObjMaps.newImmutableMap(toImmutable);
        }
        // Akarin end
        calculatePermissionDefault(perm, dirty);
    }

    @Nonnull // Akarin - javax.annotation
    public Set<Permission> getDefaultPermissions(boolean op) {
        return defaultPerms.get(op);
    }

    public void removePermission(@Nonnull Permission perm) { // Akarin - javax.annotation
        removePermission(perm.getName());
    }

    public void removePermission(@Nonnull String name) { // Akarin - javax.annotation
        // Akarin start
        synchronized (permissionsLock) {
            HashObjObjMap<String, Permission> toImmutable = HashObjObjMaps.newMutableMap(permissions);
            toImmutable.remove(name.toLowerCase(java.util.Locale.ENGLISH));
            permissions = HashObjObjMaps.newImmutableMap(toImmutable);
        }
        // Akarin end
    }

    public void recalculatePermissionDefaults(@Nonnull Permission perm) { // Akarin - javax.annotation
        if (perm != null && permissions.containsKey(perm.getName().toLowerCase(java.util.Locale.ENGLISH))) {
            // Akarin start
            HashObjObjMap<Boolean, Set<Permission>> toImmutable = HashObjObjMaps.newUpdatableMap(defaultPerms);
            Set<Permission> toImmutableValueOp = HashObjSets.newMutableSet(defaultPerms.get(Boolean.TRUE));
            toImmutableValueOp.remove(perm);
            toImmutable.put(Boolean.TRUE, HashObjSets.newImmutableSet(toImmutableValueOp));

            Set<Permission> toImmutableValue = HashObjSets.newMutableSet(defaultPerms.get(Boolean.FALSE));
            toImmutableValue.remove(perm);
            toImmutable.put(Boolean.FALSE, HashObjSets.newImmutableSet(toImmutableValue));

            defaultPerms = toImmutable;
            // Akarin end

            calculatePermissionDefault(perm, true);
        }
    }

    private void calculatePermissionDefault(@Nonnull Permission perm, boolean dirty) { // Akarin - javax.annotation
        if ((perm.getDefault() == PermissionDefault.OP) || (perm.getDefault() == PermissionDefault.TRUE)) {
            // Akarin start
            HashObjObjMap<Boolean, Set<Permission>> toImmutable = HashObjObjMaps.newUpdatableMap(defaultPerms);
            Set<Permission> toImmutableValue = HashObjSets.newUpdatableSet(defaultPerms.get(Boolean.TRUE));
            toImmutableValue.add(perm);
            toImmutable.put(Boolean.TRUE, HashObjSets.newImmutableSet(toImmutableValue));
            defaultPerms = toImmutable;
            // Akarin end
            if (dirty) {
                dirtyPermissibles(true);
            }
        }
        if ((perm.getDefault() == PermissionDefault.NOT_OP) || (perm.getDefault() == PermissionDefault.TRUE)) {
            // Akarin start
            HashObjObjMap<Boolean, Set<Permission>> toImmutable = HashObjObjMaps.newUpdatableMap(defaultPerms);
            Set<Permission> toImmutableValue = HashObjSets.newUpdatableSet(defaultPerms.get(Boolean.FALSE));
            toImmutableValue.add(perm);
            toImmutable.put(Boolean.FALSE, HashObjSets.newImmutableSet(toImmutableValue));
            defaultPerms = toImmutable;
            // Akarin end
            if (dirty) {
                dirtyPermissibles(false);
            }
        }
    }

    @Deprecated
    public void dirtyPermissibles() {
        dirtyPermissibles(true);
        dirtyPermissibles(false);
    }

    private void dirtyPermissibles(boolean op) {
        Set<Permissible> permissibles = getDefaultPermSubscriptions(op);

        for (Permissible p : permissibles) {
            p.recalculatePermissions();
        }
    }

    public void subscribeToPermission(@Nonnull String permission, @Nonnull Permissible permissible) { // Akarin - javax.annotation
        String name = permission.toLowerCase(java.util.Locale.ENGLISH);
        synchronized (permSubsLock) { // Akarin
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map == null) {
            map = new WeakHashMap<Permissible, Boolean>();
            permSubs.put(name, map);
        }

        map.put(permissible, true);
        } // Akarin
    }

    public void unsubscribeFromPermission(@Nonnull String permission, @Nonnull Permissible permissible) { // Akarin - javax.annotation
        String name = permission.toLowerCase(java.util.Locale.ENGLISH);
        synchronized (permSubsLock) { // Akarin
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map != null) {
            map.remove(permissible);

            if (map.isEmpty()) {
                permSubs.remove(name);
            }
        }
        } // Akarin
    }

    @Nonnull // Akarin - javax.annotation
    public Set<Permissible> getPermissionSubscriptions(@Nonnull String permission) { // Akarin - javax.annotation
        String name = permission.toLowerCase(java.util.Locale.ENGLISH);
        synchronized (permSubsLock) { // Akarin
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map == null) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(map.keySet());
        }
        } // Akarin
    }

    public void subscribeToDefaultPerms(boolean op, @Nonnull Permissible permissible) { // Akarin - javax.annotation
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map == null) {
            map = new WeakHashMap<Permissible, Boolean>();
            defSubs.put(op, map);
        }

        map.put(permissible, true);
    }

    public void unsubscribeFromDefaultPerms(boolean op, @Nonnull Permissible permissible) { // Akarin - javax.annotation
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map != null) {
            map.remove(permissible);

            if (map.isEmpty()) {
                defSubs.remove(op);
            }
        }
    }

    @Nonnull // Akarin - javax.annotation
    public Set<Permissible> getDefaultPermSubscriptions(boolean op) {
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map == null) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(map.keySet());
        }
    }

    @Nonnull // Akarin - javax.annotation
    public Set<Permission> getPermissions() {
        return new HashSet<Permission>(permissions.values());
    }

    public boolean useTimings() {
        return co.aikar.timings.Timings.isTimingsEnabled(); // Spigot
    }

    /**
     * Sets whether or not per event timing code should be used
     *
     * @param use True if per event timing code should be used
     */
    public void useTimings(boolean use) {
        co.aikar.timings.Timings.setTimingsEnabled(use); // Paper
    }

    // Paper start
    public void clearPermissions() {
        synchronized (permissionsLock) { permissions = Collections.emptyMap(); } // Akarin
        // Akarin start
        //defaultPerms.get(true).clear();
        //defaultPerms.get(false).clear();
        HashObjObjMap<Boolean, Set<Permission>> defaultPerms = HashObjObjMaps.newUpdatableMap();
        defaultPerms.put(Boolean.TRUE, HashObjSets.newUpdatableSet());
        defaultPerms.put(Boolean.FALSE, HashObjSets.newUpdatableSet());
        this.defaultPerms = defaultPerms;
        // Akarin end
    }
    // Paper end

}
