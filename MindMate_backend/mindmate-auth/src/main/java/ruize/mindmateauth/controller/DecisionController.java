package ruize.mindmateauth.controller;

import org.springframework.web.bind.annotation.*;
import ruize.mindmateauth.dto.*;
import ruize.mindmateauth.result.Result;
import ruize.mindmateauth.service.DecisionService;

import javax.validation.Valid;
import java.util.List;

/**
 * 当前用户的决策空间接口。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@RestController
@RequestMapping("/decisions")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @GetMapping("/config")
    public Result<DecisionConfigVO> getConfig() {
        return Result.success(decisionService.getConfig());
    }

    @GetMapping("/preferences")
    public Result<DecisionPreferenceVO> getPreferences() {
        return Result.success(decisionService.getPreferences());
    }

    @PutMapping("/preferences")
    public Result<DecisionPreferenceVO> savePreferences(@Valid @RequestBody DecisionPreferenceRequest request) {
        return Result.success(decisionService.savePreferences(request));
    }

    @DeleteMapping("/preferences")
    public Result<Void> clearPreferences() {
        decisionService.clearPreferences();
        return Result.success();
    }

    @GetMapping
    public Result<List<DecisionSessionVO>> listSessions() {
        return Result.success(decisionService.listSessions());
    }

    @PostMapping
    public Result<DecisionSessionVO> createSession(@Valid @RequestBody DecisionCreateRequest request) {
        return Result.success(decisionService.createSession(request));
    }

    @GetMapping("/{id}/messages")
    public Result<List<DecisionMessageVO>> listMessages(@PathVariable("id") Long id) {
        return Result.success(decisionService.listMessages(id));
    }

    @PostMapping("/{id}/messages")
    public Result<DecisionRoundVO> sendMessage(@PathVariable("id") Long id,
                                               @Valid @RequestBody DecisionMessageRequest request) {
        return Result.success(decisionService.sendMessage(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteSession(@PathVariable("id") Long id) {
        decisionService.deleteSession(id);
        return Result.success();
    }
}
