// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Service;

import java.util.List;
import java.util.function.Function;
import java.util.Arrays;
import java.lang.reflect.Field;
import java.util.Iterator;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ListingService
{
    public <T> void createListing(final Class<T> entityClass, final Map<String, Object> fields, final JpaRepository<T, ?> repo) {
        try {
            this.validateFields(fields, entityClass);
            final T entity = entityClass.getDeclaredConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            for (final Map.Entry<String, Object> entry : fields.entrySet()) {
                final String fieldName = entry.getKey();
                final Object value = entry.getValue();
                final Field declaredField = entityClass.getDeclaredField(fieldName);
                declaredField.setAccessible(true);
                declaredField.set(entity, value);
            }
            repo.save((Object)entity);
        }
        catch (final Exception e) {
            throw new RuntimeException("Failed to create Listing of type: " + entityClass.getSimpleName(), (Throwable)e);
        }
    }
    
    private void validateFields(final Map<String, Object> fields, final Class<?> entity) {
        final List<String> fieldsList = Arrays.stream(entity.getDeclaredFields()).map((Function<? super Field, ? extends String>)Field::getName).toList();
        if (fieldsList.size() - 1 != fields.size()) {
            throw new IllegalArgumentException("Not enough information");
        }
        for (final Map.Entry<String, Object> entry : fields.entrySet()) {
            final String fieldName = entry.getKey();
            final Object value = entry.getValue();
            if (!fieldsList.contains(fieldName)) {
                throw new IllegalArgumentException("Invalid field name: " + fieldName);
            }
            try {
                final Field declaredField = entity.getDeclaredField(fieldName);
                final Class<?> fieldType = declaredField.getType();
                if (value != null && !fieldType.isInstance(value)) {
                    throw new IllegalArgumentException("Field '" + fieldName + "' expects type " + fieldType.getSimpleName() + ", but got " + value.getClass().getSimpleName());
                }
                continue;
            }
            catch (final NoSuchFieldException e) {
                throw new RuntimeException("Field not found: " + fieldName, (Throwable)e);
            }
        }
    }
}
