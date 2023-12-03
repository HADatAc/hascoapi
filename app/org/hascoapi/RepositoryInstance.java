package org.hascoapi;

import org.hascoapi.entity.pojo.Repository;

public class RepositoryInstance {

    private static Repository instance = null;

    public static Repository getInstance() {
        if (instance == null) {
            instance = Repository.getRepository();
            if (instance == null) {
                instance = new Repository();
                instance.save();
                System.out.println("Brand new repository metadata has been created.");
            } else {
                System.out.println("Existing repository metadata has been retrieved.");
            }
        }
        return instance;
    }


}