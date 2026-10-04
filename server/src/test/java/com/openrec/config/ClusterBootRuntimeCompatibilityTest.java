package com.openrec.config;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(value = "cluster", inheritProfiles = false)
public class ClusterBootRuntimeCompatibilityTest extends BootRuntimeCompatibilityTest {}
