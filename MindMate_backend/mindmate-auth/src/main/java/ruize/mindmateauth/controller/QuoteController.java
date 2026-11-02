package ruize.mindmateauth.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ruize.mindmateauth.dto.QuoteVO;
import ruize.mindmateauth.result.Result;
import ruize.mindmateauth.service.QuoteService;

import java.util.List;

/**
 * 当前登录用户的经典语录接口。
 * 用户身份一律取自登录上下文，请求体不接受归属字段。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public Result<List<QuoteVO>> listQuotes() {
        return Result.success(quoteService.listQuotes());
    }

    /**
     * 新增语录，表单字段 content、source、file 均可空，但 content 与 file 不能同时为空。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<QuoteVO> createQuote(@RequestParam(value = "content", required = false) String content,
                                       @RequestParam(value = "source", required = false) String source,
                                       @RequestParam(value = "file", required = false) MultipartFile file) {
        return Result.success(quoteService.createQuote(content, source, file));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteQuote(@PathVariable("id") Long id) {
        quoteService.deleteQuote(id);
        return Result.success();
    }
}
