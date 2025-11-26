package com.teambind.articleserver.factory;

import com.teambind.articleserver.common.exception.CustomException;
import com.teambind.articleserver.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ArticleFactory 레지스트리
 *
 * 게시글 타입에 따른 적절한 팩토리를 제공합니다.
 * Spring의 DI를 활용하여 모든 팩토리를 자동 주입받아 관리합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleFactoryRegistry {

    private final List<ArticleFactory> articleFactories;
    private final Map<ArticleType, ArticleFactory> factoryMap = new EnumMap<>(ArticleType.class);

    /**
     * Spring 컨테이너가 빈 생성 후 자동으로 팩토리들을 등록합니다.
     */
    @PostConstruct
    public void init() {
        for (ArticleFactory factory : articleFactories) {
            ArticleType type = factory.getSupportedType();
            factoryMap.put(type, factory);
            log.info("Registered factory for type: {}", type);
        }
    }

    /**
     * 게시글 타입에 맞는 팩토리를 반환합니다.
     *
     * @param type 게시글 타입
     * @return 해당 타입의 팩토리
     * @throws CustomException 지원하지 않는 타입인 경우
     */
    public ArticleFactory getFactory(ArticleType type) {
        ArticleFactory factory = factoryMap.get(type);
        if (factory == null) {
            log.error("No factory found for type: {}", type);
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                "Unsupported article type: " + type);
        }
        return factory;
    }

    /**
     * 지원되는 모든 게시글 타입을 반환합니다.
     *
     * @return 지원되는 타입 목록
     */
    public ArticleType[] getSupportedTypes() {
        return factoryMap.keySet().toArray(new ArticleType[0]);
    }
}
