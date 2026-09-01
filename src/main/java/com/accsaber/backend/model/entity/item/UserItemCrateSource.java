package com.accsaber.backend.model.entity.item;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Immutable
@Table(name = "user_item_crate_sources")
@Getter
public class UserItemCrateSource {

    @Id
    @Column(name = "source_id")
    private String sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crate_item_id")
    private Item crateItem;
}
