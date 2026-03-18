package com.accsaber.backend.service.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.model.dto.request.curve.CreateCurveRequest;
import com.accsaber.backend.model.dto.request.curve.UpdateCurveRequest;
import com.accsaber.backend.model.dto.response.CurveResponse;
import com.accsaber.backend.model.entity.Curve;
import com.accsaber.backend.repository.CurveRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurveService {

    private final CurveRepository curveRepository;

    public List<CurveResponse> findAllActive() {
        return curveRepository.findByActiveTrue().stream()
                .map(CurveService::toResponse)
                .toList();
    }

    public CurveResponse findById(UUID id) {
        Curve curve = curveRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curve", id));
        return toResponse(curve);
    }

    @Transactional
    public CurveResponse createCurve(CreateCurveRequest request) {
        Curve curve = Curve.builder()
                .name(request.getName())
                .type(request.getType())
                .formula(request.getFormula())
                .xParameterName(request.getXParameterName())
                .xParameterValue(request.getXParameterValue())
                .yParameterName(request.getYParameterName())
                .yParameterValue(request.getYParameterValue())
                .zParameterName(request.getZParameterName())
                .zParameterValue(request.getZParameterValue())
                .scale(request.getScale())
                .shift(request.getShift())
                .build();
        return toResponse(curveRepository.save(curve));
    }

    @Transactional
    public CurveResponse updateCurve(UUID id, UpdateCurveRequest request) {
        Curve curve = curveRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curve", id));

        if (request.getName() != null)
            curve.setName(request.getName());
        if (request.getFormula() != null)
            curve.setFormula(request.getFormula());
        if (request.getXParameterName() != null)
            curve.setXParameterName(request.getXParameterName());
        if (request.getXParameterValue() != null)
            curve.setXParameterValue(request.getXParameterValue());
        if (request.getYParameterName() != null)
            curve.setYParameterName(request.getYParameterName());
        if (request.getYParameterValue() != null)
            curve.setYParameterValue(request.getYParameterValue());
        if (request.getZParameterName() != null)
            curve.setZParameterName(request.getZParameterName());
        if (request.getZParameterValue() != null)
            curve.setZParameterValue(request.getZParameterValue());
        if (request.getScale() != null)
            curve.setScale(request.getScale());
        if (request.getShift() != null)
            curve.setShift(request.getShift());

        return toResponse(curveRepository.save(curve));
    }

    public static CurveResponse toResponse(Curve curve) {
        if (curve == null) {
            return null;
        }
        return CurveResponse.builder()
                .id(curve.getId())
                .name(curve.getName())
                .type(curve.getType().name())
                .formula(curve.getFormula())
                .xParameterName(curve.getXParameterName())
                .xParameterValue(curve.getXParameterValue())
                .yParameterName(curve.getYParameterName())
                .yParameterValue(curve.getYParameterValue())
                .zParameterName(curve.getZParameterName())
                .zParameterValue(curve.getZParameterValue())
                .scale(curve.getScale())
                .shift(curve.getShift())
                .build();
    }
}
