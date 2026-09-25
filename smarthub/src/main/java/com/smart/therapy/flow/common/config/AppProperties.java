package com.smart.therapy.flow.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Pagination pagination = new Pagination();
    private Billing billing = new Billing();
    private Client client = new Client();

    @Data
    public static class Pagination {
        private int defaultPage = 1;
        private int defaultPageSize = 25;
        private int maxPageSize = 500;
        private int minPageSize = 1;
    }

    @Data
    public static class Billing {
        private Discount discount = new Discount();
        private Invoice invoice = new Invoice();

        @Data
        public static class Discount {
            private int maxPercentage = 100;
            private int roundingScale = 6;
        }

        @Data
        public static class Invoice {
            private int paymentTermsDays = 30;
            private int amountRoundingScale = 2;
        }
    }

    @Data
    public static class Client {
        private int minAge = 18;
    }
}
