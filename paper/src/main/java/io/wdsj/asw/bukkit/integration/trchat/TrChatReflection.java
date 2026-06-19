package io.wdsj.asw.bukkit.integration.trchat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class TrChatReflection {
    static final String PLUGIN_NAME = "TrChat";
    private static final String SEND_EVENT_CLASS_NAME = "me.arasple.mc.trchat.api.event.TrChatSendEvent";
    private static final String COMPONENT_MANAGER_CLASS_NAME = "me.arasple.mc.trchat.api.impl.BukkitComponentManager";

    static final ClassLoader TRCHAT_CLASS_LOADER = findTrChatClassLoader();
    static final Class<? extends Event> SEND_EVENT_CLASS = findSendEventClass();
    static final Class<?> COMPONENT_MANAGER_CLASS = findClass(COMPONENT_MANAGER_CLASS_NAME);
    static final Field COMPONENT_MANAGER_INSTANCE_FIELD = findField(COMPONENT_MANAGER_CLASS, "INSTANCE");
    static final Method GET_PLAYER_METHOD = findMethod(SEND_EVENT_CLASS, "getPlayer");
    static final Method GET_COMPONENT_METHOD = findMethod(SEND_EVENT_CLASS, "getComponent");
    static final Method GET_TYPE_METHOD = findMethod(SEND_EVENT_CLASS, "getType");
    static final Method GET_MESSAGE_METHOD = findMethod(SEND_EVENT_CLASS, "getMessage");
    static final Method SEND_COMPONENT_METHOD = findMethodByShape(COMPONENT_MANAGER_CLASS, "sendComponent", 3);

    private TrChatReflection() {
    }

    static boolean isAvailable() {
        return SEND_EVENT_CLASS != null
                && COMPONENT_MANAGER_CLASS != null
                && COMPONENT_MANAGER_INSTANCE_FIELD != null
                && GET_PLAYER_METHOD != null
                && GET_COMPONENT_METHOD != null
                && GET_TYPE_METHOD != null
                && SEND_COMPONENT_METHOD != null;
    }

    static Player getPlayer(Event event) {
        try {
            Object player = GET_PLAYER_METHOD.invoke(event);
            return player instanceof Player ? (Player) player : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static Object getComponent(Event event) {
        try {
            return GET_COMPONENT_METHOD.invoke(event);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static String getTypeName(Event event) {
        try {
            Object type = GET_TYPE_METHOD.invoke(event);
            return type == null ? "" : type.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    static String getLegacyMessage(Event event) {
        if (GET_MESSAGE_METHOD == null) {
            return "";
        }
        try {
            Object message = GET_MESSAGE_METHOD.invoke(event);
            return message == null ? "" : message.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    static boolean sendComponent(Object receiver, Object component, Object sender) {
        try {
            Object manager = COMPONENT_MANAGER_INSTANCE_FIELD.get(null);
            SEND_COMPONENT_METHOD.invoke(manager, receiver, component, sender);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    static boolean isSendEvent(Event event) {
        return SEND_EVENT_CLASS != null
                && SEND_EVENT_CLASS.isInstance(event);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Event> findSendEventClass() {
        Class<?> eventClass = findClass(SEND_EVENT_CLASS_NAME);
        if (eventClass == null || !Event.class.isAssignableFrom(eventClass)) {
            return null;
        }
        return (Class<? extends Event>) eventClass;
    }

    private static ClassLoader findTrChatClassLoader() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        return plugin == null ? null : plugin.getClass().getClassLoader();
    }

    private static Class<?> findClass(String className) {
        if (TRCHAT_CLASS_LOADER == null) {
            return null;
        }
        try {
            return Class.forName(className, false, TRCHAT_CLASS_LOADER);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        if (type == null) {
            return null;
        }
        try {
            Field field = type.getField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | SecurityException ignored) {
            return null;
        }
    }

    private static Method findMethodByShape(Class<?> type, String name, int parameterCount) {
        if (type == null) {
            return null;
        }
        try {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
        } catch (SecurityException | LinkageError ignored) {
            return null;
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null) {
            return null;
        }
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) {
                return null;
            }
        }
        try {
            Method method = type.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException | SecurityException ignored) {
            return null;
        }
    }
}
