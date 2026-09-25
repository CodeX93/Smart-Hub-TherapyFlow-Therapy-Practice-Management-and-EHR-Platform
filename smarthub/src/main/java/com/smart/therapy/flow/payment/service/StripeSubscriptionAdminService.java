package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.net.RequestOptions;
import com.stripe.param.SubscriptionUpdateParams;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeSubscriptionAdminService {

    private final StripePlatformConfigService stripePlatformConfigService;
    private final OrgStripeConnectService orgStripeConnectService;

    public void updateSubscriptionPrice(Long organisationId,
                                        String providerSubscriptionId,
                                        String priceId,
                                        boolean prorate) {
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PRORATION_NOT_SUPPORTED",
                    "Provider subscription id required for Stripe update");
        }
        if (priceId == null || priceId.isBlank()) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PRICE_ID_MISSING",
                    "Provider price id required for Stripe update");
        }

        String platformKey = stripePlatformConfigService.requirePlatformSecretKey();
        Optional<String> connectAccountId = orgStripeConnectService.findConnectAccountId(organisationId);
        RequestOptions.RequestOptionsBuilder optionsBuilder = RequestOptions.builder().setApiKey(platformKey);
        connectAccountId.ifPresent(optionsBuilder::setStripeAccount);
        RequestOptions requestOptions = optionsBuilder.build();

        try {
            Subscription subscription = Subscription.retrieve(providerSubscriptionId, requestOptions);
            String itemId = resolvePrimaryItemId(subscription);
            if (itemId == null) {
                throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SUBSCRIPTION_ITEM_NOT_FOUND",
                        "Stripe subscription items missing for update");
            }

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setProrationBehavior(prorate
                            ? SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS
                            : SubscriptionUpdateParams.ProrationBehavior.NONE)
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(itemId)
                            .setPrice(priceId)
                            .build())
                    .build();
            subscription.update(params, requestOptions);
        } catch (StoryApiException ex) {
            throw ex;
        } catch (StripeException ex) {
            log.error("Stripe subscription update failed for subscriptionId={}: {}", providerSubscriptionId, ex.getMessage());
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_UNAVAILABLE",
                    "Stripe subscription update failed");
        }
    }

    private String resolvePrimaryItemId(Subscription subscription) {
        if (subscription == null
                || subscription.getItems() == null
                || subscription.getItems().getData() == null
                || subscription.getItems().getData().isEmpty()) {
            return null;
        }
        SubscriptionItem item = subscription.getItems().getData().get(0);
        return item != null ? item.getId() : null;
    }
}
