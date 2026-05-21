package com.sparta.logistics.hub;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.sparta.logistics.arch.ArchTestSupport;

@AnalyzeClasses(
        packages = "com.sparta.logistics.hub.hub",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class HubServiceArchTest extends ArchTestSupport {

    @Override
    protected String getBasePackage() {
        return "com.sparta.logistics.hub.hub";
    }
}
