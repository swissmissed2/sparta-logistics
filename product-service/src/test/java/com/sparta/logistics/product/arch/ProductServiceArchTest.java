package com.sparta.logistics.product.arch;

import com.sparta.logistics.arch.ArchTestSupport;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "com.sparta.logistics.product", importOptions = ImportOption.DoNotIncludeTests.class)
public class ProductServiceArchTest extends ArchTestSupport {
    @Override
    protected String getBasePackage(){
        return "com.sparta.logistics.product";
    }
}

