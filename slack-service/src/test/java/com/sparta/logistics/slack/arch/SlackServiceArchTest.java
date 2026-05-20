package com.sparta.logistics.slack.arch;

import com.sparta.logistics.arch.ArchTestSupport;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "com.sparta.logistics.slack", importOptions = ImportOption.DoNotIncludeTests.class)
public class SlackServiceArchTest extends ArchTestSupport {
    @Override
    protected String getBasePackage(){
        return "com.sparta.logistics.slack";
    }
}
