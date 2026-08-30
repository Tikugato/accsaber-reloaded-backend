package com.accsaber.backend.service.milestone.source;

import java.util.List;

import org.springframework.stereotype.Component;

import com.accsaber.backend.model.entity.item.ItemRarity;
import com.accsaber.backend.model.entity.item.ItemSource;
import com.accsaber.backend.model.entity.item.TradeStatus;
import com.accsaber.backend.model.entity.market.MarketListingStatus;

@Component
public class ItemSources implements MilestoneSourceProvider {

    @Override
    public List<MilestoneSource> sources() {
        return List.of(userItems(), crateOpens(), disintegrations(), trades(), tradesReceived(), marketListings(),
                marketPurchases());
    }

    private MilestoneSource userItems() {
        return MilestoneSource.named("user_items", "user_item_links", "uil")
                .triggeredBy(MilestoneTrigger.ITEM, MilestoneTrigger.MARKET, MilestoneTrigger.MISSION,
                        MilestoneTrigger.CAMPAIGN)
                .join("itm", "JOIN items {itm} ON {base}.item_id = {itm}.id")
                .join("ity", "JOIN item_types {ity} ON {itm}.type_id = {ity}.id")
                .join("imd", "LEFT JOIN item_modifiers {imd} ON {base}.modifier_id = {imd}.id")
                .user("{base}.user_id")
                .uuid("id", "{base}.id")
                .uuid("item_id", "{base}.item_id")
                .bigint("serial_number", "{base}.serial_number")
                .bigint("quantity", "{base}.quantity")
                .enumeration("source", "{base}.source", ItemSource.class)
                .uuid("unusual_effect_id", "{base}.unusual_effect_id")
                .uuid("modifier_id", "{base}.modifier_id")
                .text("modifier_key", "{imd}.key")
                .timestamp("awarded_at", "{base}.awarded_at")
                .text("item_name", "{itm}.name")
                .enumeration("item_rarity", "{itm}.rarity", ItemRarity.class)
                .flag("item_tradeable", "{itm}.tradeable")
                .bigint("item_worth", "{itm}.worth")
                .text("item_type_key", "{ity}.key")
                .text("item_type_name", "{ity}.name")
                .build();
    }

    private MilestoneSource crateOpens() {
        return MilestoneSource.named("crate_opens", "user_crate_opens", "uco")
                .triggeredBy(MilestoneTrigger.ITEM)
                .join("itm", "JOIN items {itm} ON {base}.reward_item_id = {itm}.id")
                .user("{base}.user_id")
                .uuid("id", "{base}.id")
                .uuid("crate_item_id", "{base}.crate_item_id")
                .uuid("reward_item_id", "{base}.reward_item_id")
                .timestamp("rolled_at", "{base}.rolled_at")
                .text("reward_item_name", "{itm}.name")
                .enumeration("reward_item_rarity", "{itm}.rarity", ItemRarity.class)
                .bigint("reward_item_worth", "{itm}.worth")
                .build();
    }

    private MilestoneSource disintegrations() {
        return MilestoneSource.named("disintegrations", "user_item_disintegrations", "uid")
                .triggeredBy(MilestoneTrigger.ITEM)
                .join("itm", "JOIN items {itm} ON {base}.item_id = {itm}.id")
                .user("{base}.user_id")
                .uuid("id", "{base}.id")
                .uuid("item_id", "{base}.item_id")
                .bigint("essence_gained", "{base}.essence_gained")
                .bigint("quantity", "{base}.quantity")
                .timestamp("disintegrated_at", "{base}.disintegrated_at")
                .enumeration("item_rarity", "{itm}.rarity", ItemRarity.class)
                .build();
    }

    private MilestoneSource trades() {
        return tradeColumns(MilestoneSource.named("trades", "user_item_trades", "uit").user("{base}.from_user_id"));
    }

    private MilestoneSource tradesReceived() {
        return tradeColumns(MilestoneSource.named("trades_received", "user_item_trades", "uitr").user("{base}.to_user_id"));
    }

    private MilestoneSource tradeColumns(MilestoneSource.Builder builder) {
        return builder
                .triggeredBy(MilestoneTrigger.ITEM)
                .uuid("id", "{base}.id")
                .bigint("from_user_id", "{base}.from_user_id")
                .bigint("to_user_id", "{base}.to_user_id")
                .enumeration("status", "{base}.status", TradeStatus.class)
                .bigint("offered_essence", "{base}.offered_essence")
                .bigint("requested_essence", "{base}.requested_essence")
                .timestamp("created_at", "{base}.created_at")
                .timestamp("resolved_at", "{base}.resolved_at")
                .build();
    }

    private MilestoneSource marketListings() {
        return listingColumns(MilestoneSource.named("market_listings", "market_listings", "mkl").user("{base}.seller_id"));
    }

    private MilestoneSource marketPurchases() {
        return listingColumns(MilestoneSource.named("market_purchases", "market_listings", "mkp").user("{base}.winner_id"));
    }

    private MilestoneSource listingColumns(MilestoneSource.Builder builder) {
        return builder
                .triggeredBy(MilestoneTrigger.MARKET)
                .join("itm", "JOIN items {itm} ON {base}.item_id = {itm}.id")
                .uuid("id", "{base}.id")
                .bigint("seller_id", "{base}.seller_id")
                .bigint("winner_id", "{base}.winner_id")
                .uuid("item_id", "{base}.item_id")
                .enumeration("status", "{base}.status", MarketListingStatus.class)
                .bigint("final_price", "{base}.final_price")
                .bigint("current_bid", "{base}.current_bid")
                .bigint("starting_bid", "{base}.starting_bid")
                .bigint("buyout_price", "{base}.buyout_price")
                .bigint("quantity", "{base}.quantity")
                .timestamp("created_at", "{base}.created_at")
                .timestamp("ends_at", "{base}.ends_at")
                .timestamp("settled_at", "{base}.settled_at")
                .text("item_name", "{itm}.name")
                .enumeration("item_rarity", "{itm}.rarity", ItemRarity.class)
                .build();
    }
}
