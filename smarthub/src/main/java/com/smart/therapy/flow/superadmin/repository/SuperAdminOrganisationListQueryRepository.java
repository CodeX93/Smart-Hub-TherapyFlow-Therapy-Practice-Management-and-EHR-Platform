package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListResult;
public interface SuperAdminOrganisationListQueryRepository {

    SuperAdminOrganisationListResult search(SuperAdminOrganisationListRequest req);
}
