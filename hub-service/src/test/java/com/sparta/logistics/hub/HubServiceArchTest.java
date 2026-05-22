package com.sparta.logistics.hub;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.sparta.logistics.arch.ArchTestSupport;

@AnalyzeClasses(
        // 패키지 스캔 범위를 상위 패키지로 하여 하위의 4개 도메인을 모두 포함시킵니다.
        packages = "com.sparta.logistics.hub",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class HubServiceArchTest extends ArchTestSupport {

    @Override
    protected String getBasePackage() {
        return "com.sparta.logistics.hub";
    }
}
