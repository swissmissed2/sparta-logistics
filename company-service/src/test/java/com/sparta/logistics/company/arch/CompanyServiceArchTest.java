package com.sparta.logistics.company.arch;

import com.sparta.logistics.arch.ArchTestSupport;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "com.sparta.logistics.company", importOptions = ImportOption.DoNotIncludeTests.class)
public class CompanyServiceArchTest extends ArchTestSupport {
    @Override
    protected String getBasePackage(){
        return "com.sparta.logistics.company";
    }
}