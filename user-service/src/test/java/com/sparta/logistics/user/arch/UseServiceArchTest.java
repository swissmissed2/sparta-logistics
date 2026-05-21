package com.sparta.logistics.user.arch;

import com.sparta.logistics.arch.ArchTestSupport;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "com.sparta.logistics.user", importOptions = ImportOption.DoNotIncludeTests.class)
public class UseServiceArchTest extends ArchTestSupport {
    @Override
    protected String getBasePackage(){
        return "com.sparta.logistics.user";
    }
}
