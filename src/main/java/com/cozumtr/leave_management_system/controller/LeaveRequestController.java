package com.cozumtr.leave_management_system.controller;
import com.cozumtr.leave_management_system.dto.response.LeaveTimelineDto;
import com.cozumtr.leave_management_system.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService  leaveRequestService;

    @PostMapping
    public String createLeave() {
        return "İzin talep etme ucu...";
    }

    @GetMapping("/my-leaves")
    public String getMyLeaves() {
        return "Geçmiş izinlerim listesi...";
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<LeaveTimelineDto>> getLeaveTimeline(@PathVariable Long id){
        return ResponseEntity.ok(leaveRequestService.getRequestTimeline(id));
    }
}
