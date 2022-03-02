package nextstep.subway.ui;

import java.util.Objects;
import java.util.Optional;

import nextstep.auth.authorization.AuthenticationPrincipal;
import nextstep.member.domain.LoginMember;
import nextstep.subway.applicaion.PathService;
import nextstep.subway.applicaion.dto.PathResponse;
import nextstep.subway.domain.WeightType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PathController {
    private PathService pathService;

    public PathController(PathService pathService) {
        this.pathService = pathService;
    }

    @GetMapping("/paths")
    public ResponseEntity<PathResponse> findPath(@AuthenticationPrincipal LoginMember loginMember
                                                , @RequestParam Long source
                                                , @RequestParam Long target
                                                , @RequestParam WeightType weightType) {

        return ResponseEntity.ok(pathService.findPath(
                    Objects.isNull(loginMember) ? -1 : loginMember.getAge()
                    , source
                    , target
                    , weightType));
    }
}
