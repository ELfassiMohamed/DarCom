package com.backend.repository;

import com.backend.domain.Photo;
import jakarta.persistence.EntityManager;

public class PhotoRepository extends GenericRepository<Photo> {

    public PhotoRepository(EntityManager em) {
        super(em, Photo.class);
    }
}
