@FilterDefs({
  @FilterDef(
      name = "tenant_filter",
      parameters = {@ParamDef(name = "assessoriaId", type = UUID.class)})
})
package com.hubfeatcreators.domain;

import java.util.UUID;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.ParamDef;
