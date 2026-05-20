package com.sparta.logistics.order.arch;

import com.sparta.logistics.arch.ArchTestSupport;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "com.spara.logistics.order", importOptions = ImportOption.DoNotIncludeTests.class)
public class OrderServiceArchTest extends ArchTestSupport {
    @Override
    protected String getBasePackage(){
        return "com.sparta.logistics.order";
    }
}
