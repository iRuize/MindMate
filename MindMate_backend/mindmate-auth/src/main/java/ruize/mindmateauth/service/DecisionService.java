package ruize.mindmateauth.service;

import ruize.mindmateauth.dto.*;

import java.util.List;

/**
 * 决策空间业务接口。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
public interface DecisionService {

    DecisionConfigVO getConfig();

    DecisionPreferenceVO getPreferences();

    DecisionPreferenceVO savePreferences(DecisionPreferenceRequest request);

    void clearPreferences();

    List<DecisionSessionVO> listSessions();

    DecisionSessionVO createSession(DecisionCreateRequest request);

    List<DecisionMessageVO> listMessages(Long sessionId);

    DecisionRoundVO sendMessage(Long sessionId, DecisionMessageRequest request);

    void deleteSession(Long sessionId);
}
