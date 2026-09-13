package com.openrec.graph.node;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;

/** Immutable reflection metadata shared by graph compilation and node execution. */
public final class NodeContract {

    private final List<Port> imports;
    private final List<Port> exports;

    private NodeContract(List<Port> imports, List<Port> exports) {
        this.imports = Collections.unmodifiableList(imports);
        this.exports = Collections.unmodifiableList(exports);
    }

    public static NodeContract inspect(Class<?> nodeClass) {
        Map<String, Port> imports = new LinkedHashMap<>();
        Map<String, Port> exports = new LinkedHashMap<>();
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> type = nodeClass; type != null && type != Object.class; type = type.getSuperclass())
            hierarchy.add(0, type);
        for (Class<?> type : hierarchy) {
            for (Field field : type.getDeclaredFields()) {
                Import imported = field.getAnnotation(Import.class);
                Export exported = field.getAnnotation(Export.class);
                if (imported != null && exported != null)
                    throw new IllegalArgumentException(fieldName(field) + " cannot be both @Import and @Export");
                if (imported != null)
                    add(imports, new Port(imported.value(), field, imported.required()), "import", nodeClass);
                if (exported != null)
                    add(exports, new Port(exported.value(), field, true), "export", nodeClass);
            }
        }
        return new NodeContract(new ArrayList<>(imports.values()), new ArrayList<>(exports.values()));
    }

    private static void add(Map<String, Port> ports, Port port, String direction, Class<?> nodeClass) {
        if (port.name.trim().isEmpty())
            throw new IllegalArgumentException(nodeClass.getName() + " has a blank " + direction + " name");
        Port previous = ports.put(port.name, port);
        if (previous != null)
            throw new IllegalArgumentException(
                nodeClass.getName() + " declares duplicate " + direction + ": " + port.name);
    }

    private static String fieldName(Field field) {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    public List<Port> imports() {
        return imports;
    }

    public List<Port> exports() {
        return exports;
    }

    public static final class Port {
        private final String name;
        private final Field field;
        private final boolean required;

        private Port(String name, Field field, boolean required) {
            this.name = name == null ? "" : name.trim();
            this.field = field;
            this.required = required;
            this.field.setAccessible(true);
        }

        public String name() {
            return name;
        }

        public Type type() {
            return field.getGenericType();
        }

        public Class<?> rawType() {
            return field.getType();
        }

        public boolean required() {
            return required;
        }

        public Object read(Object target) {
            try {
                return field.get(target);
            } catch (IllegalAccessException error) {
                throw new IllegalStateException("cannot read graph export " + name + " from " + fieldName(field),
                    error);
            }
        }

        public void write(Object target, Object value) {
            if (value != null && !rawType().isInstance(value))
                throw new IllegalArgumentException("graph import " + name + " expects " + type().getTypeName()
                    + " but received " + value.getClass().getTypeName());
            try {
                field.set(target, value);
            } catch (IllegalAccessException error) {
                throw new IllegalStateException("cannot write graph import " + name + " to " + fieldName(field), error);
            }
        }
    }
}
