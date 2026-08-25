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
        return List.of(userItems(), crateOpens(), disintegrations(), trades(), marketListings());
    }

    private MilestoneSource userItems() {
        return MilestoneSource.named("user_items", "user_item_links", "uil")
                .triggeredBy(MilestoneTrigger.ITEM, MilestoneTrigger.MARKET, MilestoneTrigger.MISSION,
                        MilestoneTrigger.CAMPAIGN)
                .join("itm", "JOIN items {itm} ON {base}.item_id = {itm}.id")
                .join("ity", "JOIN item_types {ity} ON {itm}.type_id = {ity}.id")
                .user("{base}.user_id")
                .uuid("id", "{base}.id")
                .uuid("item_id", "{base}.item_id")
                .bigint("serial_number", "{base}.serial_number")
                .enumeration("source", "{base}.source", ItemSource.class)
                .uuid("unusual_effect_id", "{base}.unusual_effect_id")
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
        return MilestoneSource.named("trades", "user_item_trades", "uit")
                .triggeredBy(MilestoneTrigger.ITEM)
                .user("{base}.from_user_id")
                .uuid("id", "{base}.id")
                .bigint("from_user_id", "{base}.from_user_id")
                .bigint("to_user_id", "{base}.to_user_id")
                .enumeration("status", "{base}.status", TradeStatus.class)
                .bigint("offered_essence", "{base}.offered_essence")
                .bigint("requested_essence", "{base}.requested_essence")
                .timestamp("resolved_at", "{base}.resolved_at")
                .build();
    }

    private MilestoneSource marketListings() {
        return MilestoneSource.named("market_listings", "market_listings", "mkl")
                .triggeredBy(MilestoneTrigger.MARKET)
                .join("itm", "JOIN items {itm} ON {base}.item_id = {itm}.id")
                .user("{base}.seller_id")
                .uuid("id", "{base}.id")
                .bigint("seller_id", "{base}.seller_id")
                .bigint("winner_id", "{base}.winner_id")
                .uuid("item_id", "{base}.item_id")
                .enumeration("status", "{base}.status", MarketListingStatus.class)
                .bigint("final_price", "{base}.final_price")
                .bigint("current_bid", "{base}.current_bid")
                .bigint("buyout_price", "{base}.buyout_price")
                .bigint("quantity", "{base}.quantity")
                .timestamp("settled_at", "{base}.settled_at")
                .text("item_name", "{itm}.name")
                .enumeration("item_rarity", "{itm}.rarity", ItemRarity.class)
                .build();
    }
}
