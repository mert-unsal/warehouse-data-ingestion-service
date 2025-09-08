package com.ikea.warehouse_data_ingestion_service.service;

import java.lang.reflect.Field;

final class TestUtils {
    private TestUtils() {}

    static void setField(Object target, Object value) {
        try {
            Field f = target.getClass().getDeclaredField("productTopic");
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException e) {
            // search in super classes
            Class<?> c = target.getClass().getSuperclass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField("productTopic");
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    c = c.getSuperclass();
                } catch (IllegalAccessException iae) {
                    throw new RuntimeException(iae);
                }
            }
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
