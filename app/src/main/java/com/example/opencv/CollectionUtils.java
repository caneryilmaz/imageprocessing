package com.example.opencv;

import java.util.Collection;

@SuppressWarnings("DefaultFileTemplate")
public class CollectionUtils {

    public static boolean isEmpty(final Collection collection) {
        if (collection == null) {
            return true;
        }
        return collection.isEmpty();
    }

    public static boolean isNotEmpty(final Collection collection) {
        return !isEmpty(collection);
    }

    public static int size(final Collection collection) {
        if (collection == null) {
            return 0;
        }
        return collection.size();
    }

    public static int size(Object[] objects) {
        if (objects == null) {
            return 0;
        }
        return objects.length;
    }
}
