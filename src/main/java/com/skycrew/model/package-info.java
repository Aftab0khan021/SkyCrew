/**
 * Package-level Hibernate filter definitions for multi-tenancy.
 * The @FilterDef must be declared only once globally.
 */
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = String.class)
)
package com.skycrew.model;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
