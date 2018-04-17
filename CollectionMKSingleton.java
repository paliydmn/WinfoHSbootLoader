package paliy;

public class CollectionMKSingleton {

        private static volatile CollectionMK instance;

        public static CollectionMK getInstance() {
            CollectionMK localInstance = instance;
            if (localInstance == null) {
                synchronized (CollectionMKSingleton.class) {
                    localInstance = instance;
                    if (localInstance == null) {
                        instance = localInstance = new CollectionMK();
                    }
                }
            }
            return localInstance;
        }
}
