package com.accsaber.backend.service.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.model.dto.request.curve.CreateCurveRequest;
import com.accsaber.backend.model.dto.request.curve.UpdateCurveRequest;
import com.accsaber.backend.model.dto.response.CurveResponse;
import com.accsaber.backend.model.entity.Curve;
import com.accsaber.backend.model.entity.CurveType;
import com.accsaber.backend.repository.CurveRepository;

@ExtendWith(MockitoExtension.class)
class CurveServiceCreateUpdateTest {

    @Mock
    private CurveRepository curveRepository;

    @InjectMocks
    private CurveService curveService;

    @Nested
    class CreateCurve {

        @Test
        void formulaCurve_savesAllFieldsCorrectly() {
            CreateCurveRequest request = new CreateCurveRequest();
            request.setName("Test Weight Curve");
            request.setType(CurveType.FORMULA);
            request.setFormula("EXPONENTIAL_DECAY");
            request.setXParameterName("position");
            request.setXParameterValue(BigDecimal.ONE);
            request.setYParameterName("base");
            request.setYParameterValue(new BigDecimal("0.965"));

            Curve saved = Curve.builder()
                    .id(UUID.randomUUID())
                    .name("Test Weight Curve")
                    .type(CurveType.FORMULA)
                    .formula("EXPONENTIAL_DECAY")
                    .xParameterName("position")
                    .xParameterValue(BigDecimal.ONE)
                    .yParameterName("base")
                    .yParameterValue(new BigDecimal("0.965"))
                    .build();

            when(curveRepository.save(any())).thenReturn(saved);

            CurveResponse response = curveService.createCurve(request);

            assertThat(response.getName()).isEqualTo("Test Weight Curve");
            assertThat(response.getType()).isEqualTo("FORMULA");
            assertThat(response.getFormula()).isEqualTo("EXPONENTIAL_DECAY");
            assertThat(response.getXParameterName()).isEqualTo("position");
            assertThat(response.getYParameterValue()).isEqualByComparingTo(new BigDecimal("0.965"));
        }

        @Test
        void pointLookupCurve_savesScaleAndShift() {
            CreateCurveRequest request = new CreateCurveRequest();
            request.setName("Test Score Curve");
            request.setType(CurveType.POINT_LOOKUP);
            request.setScale(new BigDecimal("61"));
            request.setShift(new BigDecimal("-18"));

            Curve saved = Curve.builder()
                    .id(UUID.randomUUID())
                    .name("Test Score Curve")
                    .type(CurveType.POINT_LOOKUP)
                    .scale(new BigDecimal("61"))
                    .shift(new BigDecimal("-18"))
                    .build();

            when(curveRepository.save(any())).thenReturn(saved);

            CurveResponse response = curveService.createCurve(request);

            assertThat(response.getType()).isEqualTo("POINT_LOOKUP");
            assertThat(response.getScale()).isEqualByComparingTo(new BigDecimal("61"));
            assertThat(response.getShift()).isEqualByComparingTo(new BigDecimal("-18"));
            assertThat(response.getFormula()).isNull();
        }

        @Test
        void allParameters_areMappedToEntity() {
            CreateCurveRequest request = new CreateCurveRequest();
            request.setName("Full Curve");
            request.setType(CurveType.FORMULA);
            request.setFormula("CUSTOM");
            request.setXParameterName("x");
            request.setXParameterValue(BigDecimal.ONE);
            request.setYParameterName("y");
            request.setYParameterValue(BigDecimal.TEN);
            request.setZParameterName("z");
            request.setZParameterValue(new BigDecimal("3.14"));
            request.setScale(new BigDecimal("100"));
            request.setShift(BigDecimal.ZERO);

            ArgumentCaptor<Curve> captor = ArgumentCaptor.forClass(Curve.class);
            when(curveRepository.save(captor.capture())).thenAnswer(inv -> {
                Curve c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            curveService.createCurve(request);

            Curve captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo("Full Curve");
            assertThat(captured.getType()).isEqualTo(CurveType.FORMULA);
            assertThat(captured.getZParameterName()).isEqualTo("z");
            assertThat(captured.getZParameterValue()).isEqualByComparingTo(new BigDecimal("3.14"));
            assertThat(captured.getScale()).isEqualByComparingTo(new BigDecimal("100"));
        }
    }

    @Nested
    class UpdateCurve {

        @Test
        void partialUpdate_onlyChangesProvidedFields() {
            Curve existing = Curve.builder()
                    .id(UUID.randomUUID())
                    .name("Original")
                    .type(CurveType.FORMULA)
                    .formula("EXPONENTIAL_DECAY")
                    .xParameterName("position")
                    .xParameterValue(BigDecimal.ONE)
                    .yParameterName("base")
                    .yParameterValue(new BigDecimal("0.965"))
                    .build();

            UpdateCurveRequest request = new UpdateCurveRequest();
            request.setName("Updated Name");

            when(curveRepository.findByIdAndActiveTrue(existing.getId())).thenReturn(Optional.of(existing));
            when(curveRepository.save(any())).thenReturn(existing);

            curveService.updateCurve(existing.getId(), request);

            assertThat(existing.getName()).isEqualTo("Updated Name");
            assertThat(existing.getFormula()).isEqualTo("EXPONENTIAL_DECAY");
            assertThat(existing.getYParameterValue()).isEqualByComparingTo(new BigDecimal("0.965"));
        }

        @Test
        void updateMultipleFields_changesAllProvided() {
            Curve existing = Curve.builder()
                    .id(UUID.randomUUID())
                    .name("Weight Curve")
                    .type(CurveType.FORMULA)
                    .formula("EXPONENTIAL_DECAY")
                    .yParameterName("base")
                    .yParameterValue(new BigDecimal("0.965"))
                    .build();

            UpdateCurveRequest request = new UpdateCurveRequest();
            request.setYParameterValue(new BigDecimal("0.970"));
            request.setScale(new BigDecimal("50"));

            when(curveRepository.findByIdAndActiveTrue(existing.getId())).thenReturn(Optional.of(existing));
            when(curveRepository.save(any())).thenReturn(existing);

            curveService.updateCurve(existing.getId(), request);

            assertThat(existing.getYParameterValue()).isEqualByComparingTo(new BigDecimal("0.970"));
            assertThat(existing.getScale()).isEqualByComparingTo(new BigDecimal("50"));
            assertThat(existing.getName()).isEqualTo("Weight Curve");
        }

        @Test
        void nullFields_areNotOverwritten() {
            Curve existing = Curve.builder()
                    .id(UUID.randomUUID())
                    .name("Keep Me")
                    .type(CurveType.FORMULA)
                    .formula("EXPONENTIAL_DECAY")
                    .xParameterName("x")
                    .xParameterValue(BigDecimal.ONE)
                    .yParameterName("y")
                    .yParameterValue(BigDecimal.TEN)
                    .zParameterName("z")
                    .zParameterValue(new BigDecimal("3"))
                    .scale(new BigDecimal("100"))
                    .shift(new BigDecimal("5"))
                    .build();

            UpdateCurveRequest request = new UpdateCurveRequest();

            when(curveRepository.findByIdAndActiveTrue(existing.getId())).thenReturn(Optional.of(existing));
            when(curveRepository.save(any())).thenReturn(existing);

            curveService.updateCurve(existing.getId(), request);

            assertThat(existing.getName()).isEqualTo("Keep Me");
            assertThat(existing.getFormula()).isEqualTo("EXPONENTIAL_DECAY");
            assertThat(existing.getXParameterName()).isEqualTo("x");
            assertThat(existing.getYParameterName()).isEqualTo("y");
            assertThat(existing.getZParameterName()).isEqualTo("z");
            assertThat(existing.getScale()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(existing.getShift()).isEqualByComparingTo(new BigDecimal("5"));
        }

        @Test
        void curveNotFound_throwsResourceNotFoundException() {
            UUID id = UUID.randomUUID();
            when(curveRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

            UpdateCurveRequest request = new UpdateCurveRequest();
            request.setName("Won't Save");

            assertThatThrownBy(() -> curveService.updateCurve(id, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void updateCurve_callsSave() {
            Curve existing = Curve.builder()
                    .id(UUID.randomUUID())
                    .name("Curve")
                    .type(CurveType.FORMULA)
                    .build();

            UpdateCurveRequest request = new UpdateCurveRequest();
            request.setName("New Name");

            when(curveRepository.findByIdAndActiveTrue(existing.getId())).thenReturn(Optional.of(existing));
            when(curveRepository.save(any())).thenReturn(existing);

            curveService.updateCurve(existing.getId(), request);

            verify(curveRepository).save(existing);
        }
    }
}
