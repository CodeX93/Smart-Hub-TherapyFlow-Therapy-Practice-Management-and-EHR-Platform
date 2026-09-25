package com.smart.therapy.flow.common.cache;

import com.smart.therapy.flow.common.tenant.TenantContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CacheKeyUtil {

    public static String tenantScope() {
        String schema = TenantContext.getSchemaName();
        Long organisationId = TenantContext.getOrganisationId();
        String schemaPart = (schema != null && !schema.isBlank()) ? schema : "public";
        String orgPart = organisationId != null ? organisationId.toString() : "no-org";
        return "tenant:" + schemaPart + ":org:" + orgPart;
    }

    public static String tenantKey(Object key) {
        return tenantScope() + ":" + String.valueOf(key);
    }
}
