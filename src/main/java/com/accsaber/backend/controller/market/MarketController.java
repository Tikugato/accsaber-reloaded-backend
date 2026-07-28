package com.accsaber.backend.controller.market;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.exception.UnauthorizedException;
import com.accsaber.backend.model.dto.request.market.CreateListingRequest;
import com.accsaber.backend.model.dto.request.market.MarketFilter;
import com.accsaber.backend.model.dto.request.market.MarketKind;
import com.accsaber.backend.model.dto.request.market.MarketSortOption;
import com.accsaber.backend.model.dto.request.market.PlaceBidRequest;
import com.accsaber.backend.model.dto.response.market.MarketBidResponse;
import com.accsaber.backend.model.dto.response.market.MarketListingResponse;
import com.accsaber.backend.model.entity.item.ItemRarity;
import com.accsaber.backend.model.entity.market.MarketListingStatus;
import com.accsaber.backend.security.PlayerUserDetails;
import com.accsaber.backend.service.market.MarketBidService;
import com.accsaber.backend.service.market.MarketListingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/market")
@RequiredArgsConstructor
@Tag(name = "Items and Market")
public class MarketController {

    private final MarketListingService listingService;
    private final MarketBidService bidService;

    @Operation(summary = "Browse the market", description = "Everything currently up for sale, ending soonest first unless you say otherwise. Use kind to pick between auction and shop listings, and sortBy for ending_soon, newest, price_asc or price_desc. You can also filter on the modifier or unusual effect of the actual copy being sold, though that only matches listings whose item still exists. No sign in needed to look.")
    @GetMapping("/listings")
    public ResponseEntity<Page<MarketListingResponse>> browse(
            @RequestParam(required = false) MarketListingStatus status,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) List<String> typeKey,
            @RequestParam(required = false) List<ItemRarity> rarity,
            @RequestParam(required = false) List<String> modifierKey,
            @RequestParam(required = false) List<String> effectKey,
            @RequestParam(required = false) MarketKind kind,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MarketSortOption sortBy,
            @PageableDefault(size = 30) Pageable pageable) {
        MarketFilter filter = new MarketFilter(status, sellerId, typeKey, rarity,
                modifierKey, effectKey, kind, minPrice, maxPrice, search, sortBy);
        return ResponseEntity.ok(listingService.browse(filter, pageable));
    }

    @Operation(summary = "Get one listing", description = "A single listing with its current state. This is the one to link people to.")
    @GetMapping("/listings/{id}")
    public ResponseEntity<MarketListingResponse> findOne(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.findDetail(id));
    }

    @Operation(summary = "Get a listing's bid history", description = "Every bid placed on a listing, so you can show how it got to where it is.")
    @GetMapping("/listings/{id}/bids")
    public ResponseEntity<List<MarketBidResponse>> bids(@PathVariable UUID id) {
        return ResponseEntity.ok(bidService.findBids(id));
    }

    @Operation(summary = "Put an item up for sale", description = "Lists something you own, either as a shop listing at a fixed price or as an auction. The item is held in escrow while it is listed, so you will not be able to equip or trade it until the listing ends or you cancel.")
    @PostMapping("/listings")
    public ResponseEntity<MarketListingResponse> create(@Valid @RequestBody CreateListingRequest req,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long me = requirePrincipal(principal).getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.create(me, req));
    }

    @Operation(summary = "Cancel a listing", description = "Pulls one of your listings and gives you the item back. You can only do this while nobody has bid, since cancelling out from under a bidder would not be fair.")
    @DeleteMapping("/listings/{id}")
    public ResponseEntity<MarketListingResponse> cancel(@PathVariable UUID id,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long me = requirePrincipal(principal).getUserId();
        return ResponseEntity.ok(listingService.cancel(id, me));
    }

    @Operation(summary = "Place a bid", description = "Bids on an auction listing. Your essence is held while you are the high bidder and released if someone outbids you. Bid at or above the buyout price and it simply completes there and then rather than waiting for the auction to end.")
    @PostMapping("/listings/{id}/bids")
    public ResponseEntity<MarketListingResponse> placeBid(@PathVariable UUID id,
            @Valid @RequestBody PlaceBidRequest req,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long me = requirePrincipal(principal).getUserId();
        bidService.placeBid(id, me, req.getAmount());
        return ResponseEntity.ok(listingService.findDetail(id));
    }

    @Operation(summary = "Buy something outright", description = "Takes a listing at its buyout price and settles immediately. The item moves to you and the essence moves to the seller in one go.")
    @PostMapping("/listings/{id}/buy")
    public ResponseEntity<MarketListingResponse> buyNow(@PathVariable UUID id,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long me = requirePrincipal(principal).getUserId();
        bidService.buyNow(id, me);
        return ResponseEntity.ok(listingService.findDetail(id));
    }

    @Operation(summary = "Get your market activity", description = "Everything you have a stake in, so listings you are selling, ones you are currently winning, and ones you have already won. One call rather than three.")
    @GetMapping("/me/listings")
    public ResponseEntity<Page<MarketListingResponse>> myListings(
            @RequestParam(required = false) List<MarketListingStatus> status,
            @PageableDefault(size = 30) Pageable pageable,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long me = requirePrincipal(principal).getUserId();
        return ResponseEntity.ok(listingService.findInvolvingUser(me, status, pageable));
    }

    @Operation(summary = "Get your bid history", description = "Every bid you have placed, including on listings that have since ended.")
    @GetMapping("/me/bids")
    public ResponseEntity<Page<MarketBidResponse>> myBids(
            @PageableDefault(size = 30) Pageable pageable,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long me = requirePrincipal(principal).getUserId();
        return ResponseEntity.ok(bidService.findMyBids(me, pageable));
    }

    private PlayerUserDetails requirePrincipal(PlayerUserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        return principal;
    }
}
