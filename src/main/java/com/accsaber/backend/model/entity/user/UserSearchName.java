package com.accsaber.backend.model.entity.user;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_search_names")
@Immutable
@IdClass(UserSearchName.Key.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchName {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "search_name", nullable = false)
    private String searchName;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {

        private Long userId;

        private String searchName;

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof Key key))
                return false;
            return Objects.equals(userId, key.userId) && Objects.equals(searchName, key.searchName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, searchName);
        }
    }
}
