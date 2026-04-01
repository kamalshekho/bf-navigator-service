package com.bf.navigator.service.route.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bf.navigator.service.route.dto.TrainRouteRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


@Component
public class GoogleTrainRouteClient {

    private static final String BASE_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final String FIELD_MASK = String.join(",",
            "routes.localizedValues.distance.text",
            "routes.localizedValues.duration.text",
            "routes.legs.steps.transitDetails.stopDetails.departureStop",
            "routes.legs.steps.transitDetails.stopDetails.arrivalStop",
            "routes.legs.steps.transitDetails.stopDetails.departureTime",
            "routes.legs.steps.transitDetails.stopDetails.arrivalTime",
            "routes.legs.steps.transitDetails.transitLine.name",
            "routes.legs.steps.transitDetails.transitLine.nameShort",
            "routes.legs.steps.transitDetails.transitLine.vehicle.name.text",
            "routes.legs.steps.transitDetails.transitLine.agencies.name");

//    private static final String FIELD_MASK = "*";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;


    public GoogleTrainRouteClient(RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${google.routes.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }


    public ArrayNode computeTrainRoutes(TrainRouteRequestDTO request, boolean debug) {
        try {
            JsonNode rootNode = objectMapper.readTree(computeTrainRoutesRaw(request, FIELD_MASK, debug));
            JsonNode routesNode = rootNode.path("routes");
            return routesNode.isArray() ? (ArrayNode) routesNode : objectMapper.createArrayNode();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google Routes response", e);
        }
    }


    public String computeTrainRoutesRaw(TrainRouteRequestDTO request, String field_mask, boolean debug) {

        if (debug) {
            return DEBUG_GOOGLE_MAPS_RESPONSE;
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing Google Routes API key. Configure google.routes.api-key");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", field_mask);

        try {
            String requestBody = objectMapper.writeValueAsString(buildRequestBody(request));
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(BASE_URL, HttpMethod.POST, entity, String.class);
            System.out.println("Google API endpoint was called");
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Google Routes API", e);
        }
    }


    public String computeTrainRoutesAllData(TrainRouteRequestDTO request) {
        return computeTrainRoutesRaw(request, "*", false);
    }


    private ObjectNode buildRequestBody(TrainRouteRequestDTO request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("origin", waypoint(request.getOrigin()));
        body.set("destination", waypoint(request.getDestination()));
        body.put("travelMode", "TRANSIT");
        body.put("departureTime", request.getDepartureTime().toInstant().toString());
        body.put("computeAlternativeRoutes", false);

        ObjectNode transitPreferences = objectMapper.createObjectNode();
        ArrayNode allowedTravelModes = objectMapper.createArrayNode();
        allowedTravelModes.add("TRAIN");
        transitPreferences.set("allowedTravelModes", allowedTravelModes);
        body.set("transitPreferences", transitPreferences);
        return body;
    }


    // create a waypoint based on adress
    private ObjectNode waypoint(String address) {
        ObjectNode waypoint = objectMapper.createObjectNode();
        waypoint.put("address", address);
        return waypoint;
    }


    // debug respoce because we habe only 1000 free requests
    private static final String DEBUG_GOOGLE_MAPS_RESPONSE = """
                {
                  "routes": [
                    {
                      "legs": [
                        {
                          "distanceMeters": 240215,
                          "duration": "8411s",
                          "staticDuration": "8411s",
                          "polyline": {
                            "encodedPolyline": "gpzeIwda|@EIs@AI?IP@OEU?FmAgFEUf@W|J}DnAaAf@k@`AiBjAkDx@oDRmAZ_EPaDDaGCsASaDm@eE}@uFMkBEqCHoBPcB^aBr@eBt@kAjAgA|DmCdEeCpHuFzDcDfI_G~@g@lBq@nBY|AArBPvYnGtA\\\\x@VdCnArBzAhSpQpIvIzEfEnLbLrC|BpGpEvJ~FtD`ChB|@nJfDdHbD~DvAlPdF~Bl@nCd@`DVjDBzGObIDbLTvDV~Ex@xHxB~EdAxYzFb@FrDnAdDd@nNvAzQ~AlM|AhJ|AvKzBbG|Ah@VlN~DrN~EhTxIxRhI~DpApIjDhJrDjLxDnIjDdDnAzDnA~@TjC\\\\|@BpB@fD[pE}@hEiAnAg@vBiAbBoAfCgCdCeDp@eAXa@vT}_@jQ{YzF}JtD_GrAkB`D_EpD_ErH{GzEmErAwAfGqHdBeB~BqBdFgE~@y@tCcDxMgPdRcUtFsGxBwCpDuF|@mAtGgLf@w@d@cA^s@b@cAvDqGfIyOvAuC|BmFvAqDtEiMhHsUlEsMjF_NdF}KfBsDrByDxMoU~IqOzBqDhR_\\\\lEiInDsHpH{PxCqGzEeLjAyB`O}]~AoEv@kBbByEXo@hBmGj@mCZyCViEpA}Y|@sPnAkPzAiIlDqPxDqOhD{Ln@qBrFqOxDwJ`CsFnDsHvEyIrFkJ|DiGxIeMbG}JfFgJbDuGvJkThDqI~@gCpIoWhHgWn@_CrBoJlAsFtC_O`CiNdD}TxWekBxHui@b@qC~CaUf@wDpCiR`Gub@tFaf@rA{Jz@uF`AmF|C}OtBwJtB}HjAeE`K}_@fBiGvEaO|EuNzAyDrCcHbAgCxKmXdOy_@~FwNnOi`@xIuTfLoZbPca@pIqStWqp@jCsHfFwPjAkE~Hy\\\\`GmWnIc^|DkQlL}f@~I_`@n@yCdJo`@zG}Y`Jy_@tBiJvFsU~Nko@?]`CsJ`I_]zAsHlDsNnYcoA`i@}|B|DePzBoIjBiG`DaKpEsLdEwJfHgNtRq^fB_DrB}DbYkh@pI{OfG}KvHqNzGyL~Tsa@hA{BtA_CbDeGvA{BlA_BrBaCt@s@~@u@hGmDhCuA|EqBvBo@xE{@hAO`COnD?hHn@fI`BtCLjBNhTTrECzAExEs@nPoFx@c@|B}@bFyAdGkA|B[tDYdFSjJE|FBzJKfGYrDc@zFeAvOaErAg@dQsEfLuCdc@kLfHqB~YgHb]cJpPsEtHmCbDyAdFeCxAw@jCiBlFuDtDyCfDwCxHmHtAyAtGgGtEuD`EqChFyCtHeE~ScMjNcIhSmLlEoCtWqOhFyChAm@lHcElM{HlXsOtCqBfEkC`D_C~CgCtJiJzH{IpFqHjDgF`HiK`EyFnRqYhGoJtHsK`GsIdRcY~BuDpE_I~BcEjEwIhBwD|E_LtEcMfDgKlC_K|Lwe@hAsDbD_KxBcGjDqIrDgItDkHxEgIfFwHfe@iq@n_@}i@~Vo^vk@uy@pIsL~AcCpIcMrPyUrI_MtDyExDeElDgDns@kn@lIoHnf@gc@fH_GrEyCdEwBpDsAbA[vGyAtEi@`ESrDK`WInELvBPtH`AjVjFnYpGvF|@~Ab@x@J`Cx@zFlC~BxAxUpOpElC|JtEr}@v[jk@dSvEhBrErBlDvBzDtCpEjEzBfCvCpDjd@ln@tDlE`BfBpGdFlEnCbB|@bCdAzBx@dGzAxCf@dDZrEPnBA~EU|Di@rEgAzf@iOjQsFzImC~YgJpR_GrAe@pEcA~AS|DG~CVlB\\\\dBl@pB~@dBbAjGjEjL|IvAlA|B`CnDxE`FrIrUhe@v@zAd@x@zD`IdQz]zH~OhBbEzAbDrAdClIvPvGtMvLlVhb@zz@lBlDhFtKnJnRxFjL~GdN~GvP|@fCtBvFfH`TpOzg@dz@xsCrEhP^rAfCdIvEvOv@rChEnN|AnEbEhLvDpJ|F`NxDdIhDtGxDrGrD~FlH|Jfp@xy@vDzEjM~P|OrSvSjXlOvRfa@ph@lNrQrJrMbG~IfFhI~CvFbJjQtGhM~JbSxCzFxChG~Tdd@~FvLdNlXhDfIpDlJhDtJhFxPtCtKvBlJfErS`AvFbAlG`ChR`B|M|Gnm@rKf|@ZdC|@pGlBdLdA`GdGfXfF`RzCtJ|ClIxBtFzGlPlAjDdGbOtBvExBjErDrGhHvK~ErGzEzFb@^`HnHzD`E|eAveAjT~RzJtJbDrCpLhJ|K|GxObInmArl@nMdGxVzLzQ~IdCfAbHfD`GxCzB~@bKbF|L|FlGtChCtAtaAde@rVxLxFvCzRbJhFjClFxClGxDtIbG`FbErMtLlIdJxDpEjDnEhEbGdD`FzDtGzYth@vh@j_AjGlKpL|SjDlGhDvFvDbHl]xm@|MjU`d@zw@nOnXdEvH~IrOt^ro@jXbe@~LzTfmAxvBhZjh@nj@baAxAnCvTh`@|Vnc@rT``@~Yfh@tPbZbk@|aAnH`MhQ|ZbX~d@vExGxGtHlCdCdIrGzD~BtDlBvDzArEpApE`AnQnChFh@lDn@~o@xN``@rKtOvBjDl@hRrCbCn@jDnAjDhBtDlCrAdApEjEnArAdCjD|BrDvAlClBdE|AxDno@vfBf@bApf@buApk@nbBzJ~YHT^hB|FzPpI|VlR|j@pJlYbHzRbu@vxBnYjz@`DtJxSvm@rA~Drc@|pANz@jVxs@tHtQrUxm@|gAdsCtS`i@jJjVf`@lbAfMt[~D~KbFzOhCjJzChMfAhF|B~JrC`NdPht@p@~CbF`U~ClOjEvR~BfJnDxLvB~GlCxHzTjp@nNfb@rFxPpHvT|AdEpBhGxGtR`F|NzAtEhPtf@zHzTfG`RtC|HlRxj@fMd_@`HhSzKv\\\\bB|EfAhD~FtP|DzItFtJlCtD~E|FnAlA~ClCnIdGn@l@h@d@nAz@dBbBtJhHhNlJfCjBtCdClEpE|FtGdEzEbEpEfJ`LvCdDlNjOtGjH`[|]dKjLdBrBpBnBhMrNdIrJdHxHtDxDlF`Gd_@rb@lRdTtAlAfNxNdBhBvFrGvDdEzBhCvBpBvAbA~BhAlA`@nBZnBHxAEzBW~Bu@xAs@lBuAbBaB~@kAfAgBz@aBn@yAp@kBj@qBz@cEd@aDTmCPqDToP\\\\qK|AwZVwDX{C`AoHhRokApAqGjAkFtAoEpAiDxJeTnAuBtCeFdEsFdDyC`CgBzJ{FtD}AhBc@jB[nA]vAm@dDqBt@w@xAmAlAaB~DkGh@iA`CaG|@oBlCcHACyBaEdAvArF_O`B{D`BcFVmAl@_CRgA`@yCx@eLjAkQtAsV|AeYb@wLLiI@mIGkGa@oSAgC_Aof@KqHk@kZ}@a`@_Cm~@MuHGgJA{Q[i_@WcUWca@BiMDmGZgMXqGz@uL`@kEv@aH~@aHdKqx@pHyk@n@kFr@yHf@_JVmIBkIA_EY{PyBydAYiIo@{Kw@sIaAeIcXglBuD_XuAmMWeEa@_I_@gLUeKa@oLs@kWsAee@UcHMeHm@eUm@g[_@iMmAwj@MwPMaYGeIi@ePe@sQ[gIe@wFeAqIq@kGQmCImCAeCFaFLmCTcCJ_Aj@oDl@eCz@sCfAqCrBiDv@qA|@gAtHyHbB_CdByC~@kB`AiCp@aC`CuOdB_NhGce@jFs`@hBuNpEm]pNudALc@rKax@zFab@|AkMl@cG?e@t@oHh@yH|D__AfAyUBuAn@wMvIsqB~C_p@zDu~@fCan@jIyfBbDgs@|@eNlAkNr@oG~AyKdBkKjAiGrB}I~BeJrBwGlAsDvC}HlEmKzMa[hAqClXuo@|CoI|@kCnCwJ|BmJ|AiHbDgNjAsFzEwSRaAAAaB|HbMyj@tdA}tE|Hw]vCqMb@sBp@{DfA{HP}A`@{Ef@sLJaG?gDEgIKkI?nIScX@sHBKAAJ?`@kXXiXJoFb@oORuEfAoMnCyVx@oGtAiJ`CeMbE{QrDyMtB{IdA_FbNws@fDgPvAaGpCoJlFyOlHeTtAuDfAaC~CcGvz@kzAh_@}o@zmB}gDtDoGhl@scAzQ_\\\\fF_Jr@w@~B{ElDoGtXee@bZih@rQi[~W_e@`j@i`AnB_DjHwN|DwJlCcIjCgJvBwIt@cDl@cD~BqNtAoK~@iHx@cIf@sFx@sLLqCViIFmG?cFUuMc@yIk@yHi@}Fc@aGSeF{@kJc@_EoAmIcB_J__@whBcBeHyDiRwAmIwAcK}@qIo@eIm@gKYwHQqKCyHDaJPoJTaIpDc|@PuIBwEAeFOoJY}G[kFu@yI{C_[iAuImAiHaLel@oAsF_@yAmAsDoDwIyAqDiAgDy@aD]wAs@eEk@aF_@kFcAyYUaDc@yE}@{Fi@kCo@kCs@_CaAqC{AgD}AyC{@yAyByC[[eBaCkAsBg@q@aC_E}@iAiFeJABgDbI`@n@t@}AHa@]o@"
                          },
                          "startLocation": {
                            "latLng": {
                              "latitude": 53.5528352,
                              "longitude": 10.0053979
                            }
                          },
                          "endLocation": {
                            "latLng": {
                              "latitude": 52.252716899999996,
                              "longitude": 10.539322799999999
                            }
                          },
                          "steps": [
                            {
                              "distanceMeters": 40,
                              "staticDuration": "54s",
                              "polyline": {
                                "encodedPolyline": "gpzeIwda|@EIs@AI?"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 53.5528352,
                                  "longitude": 10.0053979
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 53.5531808,
                                  "longitude": 10.005459799999999
                                }
                              },
                              "navigationInstruction": {
                                "maneuver": "DEPART",
                                "instructions": "Nach Norden"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "40 m"
                                },
                                "staticDuration": {
                                  "text": "1 Minute"
                                }
                              },
                              "travelMode": "WALK"
                            },
                            {
                              "distanceMeters": 8,
                              "staticDuration": "10s",
                              "polyline": {
                                "encodedPolyline": "krzeIcea|@IP"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 53.5531808,
                                  "longitude": 10.005459799999999
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 53.5532295,
                                  "longitude": 10.005368299999999
                                }
                              },
                              "navigationInstruction": {
                                "maneuver": "TURN_LEFT",
                                "instructions": "Links abbiegen\\nTreppe nehmen"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "8 m"
                                },
                                "staticDuration": {
                                  "text": "1 Minute"
                                }
                              },
                              "travelMode": "WALK"
                            },
                            {
                              "distanceMeters": 5,
                              "staticDuration": "4s",
                              "polyline": {
                                "encodedPolyline": "urzeIqda|@@O"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 53.5532295,
                                  "longitude": 10.005368299999999
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 53.5532198,
                                  "longitude": 10.0054456
                                }
                              },
                              "navigationInstruction": {
                                "maneuver": "TURN_SHARP_RIGHT",
                                "instructions": "Scharf rechts"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "5 m"
                                },
                                "staticDuration": {
                                  "text": "1 Minute"
                                }
                              },
                              "travelMode": "WALK"
                            },
                            {
                              "distanceMeters": 8,
                              "staticDuration": "7s",
                              "polyline": {
                                "encodedPolyline": "srzeIaea|@EU"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 53.5532198,
                                  "longitude": 10.0054456
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 53.5532452,
                                  "longitude": 10.005561799999999
                                }
                              },
                              "navigationInstruction": {},
                              "localizedValues": {
                                "distance": {
                                  "text": "8 m"
                                },
                                "staticDuration": {
                                  "text": "1 Minute"
                                }
                              },
                              "travelMode": "WALK"
                            },
                            {
                              "distanceMeters": 87,
                              "staticDuration": "88s",
                              "polyline": {
                                "encodedPolyline": "yrzeIoea|@mAgF"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 53.553253399999996,
                                  "longitude": 10.0055245
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 53.553637099999996,
                                  "longitude": 10.0066775
                                }
                              },
                              "navigationInstruction": {
                                "instructions": "Hier einsteigen: E"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "87 m"
                                },
                                "staticDuration": {
                                  "text": "1 Minute"
                                }
                              },
                              "travelMode": "WALK"
                            },
                            {
                              "distanceMeters": 178315,
                              "staticDuration": "4740s",
                              "polyline": {
                                "encodedPolyline": "guzeIwla|@EUf@W|J}DnAaAf@k@`AiBjAkDx@oDRmAZ_EPaDDaGCsASaDm@eE}@uFMkBEqCHoBPcB^aBr@eBt@kAjAgA|DmCdEeCpHuFzDcDfI_G~@g@lBq@nBY|AArBPvYnGtA\\\\x@VdCnArBzAhSpQpIvIzEfEnLbLrC|BpGpEvJ~FtD`ChB|@nJfDdHbD~DvAlPdF~Bl@nCd@`DVjDBzGObIDbLTvDV~Ex@xHxB~EdAxYzFb@FrDnAdDd@nNvAzQ~AlM|AhJ|AvKzBbG|Ah@VlN~DrN~EhTxIxRhI~DpApIjDhJrDjLxDnIjDdDnAzDnA~@TjC\\\\|@BpB@fD[pE}@hEiAnAg@vBiAbBoAfCgCdCeDp@eAXa@vT}_@jQ{YzF}JtD_GrAkB`D_EpD_ErH{GzEmErAwAfGqHdBeB~BqBdFgE~@y@tCcDxMgPdRcUtFsGxBwCpDuF|@mAtGgLf@w@d@cA^s@b@cAvDqGfIyOvAuC|BmFvAqDtEiMhHsUlEsMjF_NdF}KfBsDrByDxMoU~IqOzBqDhR_\\\\lEiInDsHpH{PxCqGzEeLjAyB`O}]~AoEv@kBbByEXo@hBmGj@mCZyCViEpA}Y|@sPnAkPzAiIlDqPxDqOhD{Ln@qBrFqOxDwJ`CsFnDsHvEyIrFkJ|DiGxIeMbG}JfFgJbDuGvJkThDqI~@gCpIoWhHgWn@_CrBoJlAsFtC_O`CiNdD}TxWekBxHui@b@qC~CaUf@wDpCiR`Gub@tFaf@rA{Jz@uF`AmF|C}OtBwJtB}HjAeE`K}_@fBiGvEaO|EuNzAyDrCcHbAgCxKmXdOy_@~FwNnOi`@xIuTfLoZbPca@pIqStWqp@jCsHfFwPjAkE~Hy\\\\`GmWnIc^|DkQlL}f@~I_`@n@yCdJo`@zG}Y`Jy_@tBiJvFsU~Nko@?]`CsJ`I_]zAsHlDsNnYcoA`i@}|B|DePzBoIjBiG`DaKpEsLdEwJfHgNtRq^fB_DrB}DbYkh@pI{OfG}KvHqNzGyL~Tsa@hA{BtA_CbDeGvA{BlA_BrBaCt@s@~@u@hGmDhCuA|EqBvBo@xE{@hAO`COnD?hHn@fI`BtCLjBNhTTrECzAExEs@nPoFx@c@|B}@bFyAdGkA|B[tDYdFSjJE|FBzJKfGYrDc@zFeAvOaErAg@dQsEfLuCdc@kLfHqB~YgHb]cJpPsEtHmCbDyAdFeCxAw@jCiBlFuDtDyCfDwCxHmHtAyAtGgGtEuD`EqChFyCtHeE~ScMjNcIhSmLlEoCtWqOhFyChAm@lHcElM{HlXsOtCqBfEkC`D_C~CgCtJiJzH{IpFqHjDgF`HiK`EyFnRqYhGoJtHsK`GsIdRcY~BuDpE_I~BcEjEwIhBwD|E_LtEcMfDgKlC_K|Lwe@hAsDbD_KxBcGjDqIrDgItDkHxEgIfFwHfe@iq@n_@}i@~Vo^vk@uy@pIsL~AcCpIcMrPyUrI_MtDyExDeElDgDns@kn@lIoHnf@gc@fH_GrEyCdEwBpDsAbA[vGyAtEi@`ESrDK`WInELvBPtH`AjVjFnYpGvF|@~Ab@x@J`Cx@zFlC~BxAxUpOpElC|JtEr}@v[jk@dSvEhBrErBlDvBzDtCpEjEzBfCvCpDjd@ln@tDlE`BfBpGdFlEnCbB|@bCdAzBx@dGzAxCf@dDZrEPnBA~EU|Di@rEgAzf@iOjQsFzImC~YgJpR_GrAe@pEcA~AS|DG~CVlB\\\\dBl@pB~@dBbAjGjEjL|IvAlA|B`CnDxE`FrIrUhe@v@zAd@x@zD`IdQz]zH~OhBbEzAbDrAdClIvPvGtMvLlVhb@zz@lBlDhFtKnJnRxFjL~GdN~GvP|@fCtBvFfH`TpOzg@dz@xsCrEhP^rAfCdIvEvOv@rChEnN|AnEbEhLvDpJ|F`NxDdIhDtGxDrGrD~FlH|Jfp@xy@vDzEjM~P|OrSvSjXlOvRfa@ph@lNrQrJrMbG~IfFhI~CvFbJjQtGhM~JbSxCzFxChG~Tdd@~FvLdNlXhDfIpDlJhDtJhFxPtCtKvBlJfErS`AvFbAlG`ChR`B|M|Gnm@rKf|@ZdC|@pGlBdLdA`GdGfXfF`RzCtJ|ClIxBtFzGlPlAjDdGbOtBvExBjErDrGhHvK~ErGzEzFb@^`HnHzD`E|eAveAjT~RzJtJbDrCpLhJ|K|GxObInmArl@nMdGxVzLzQ~IdCfAbHfD`GxCzB~@bKbF|L|FlGtChCtAtaAde@rVxLxFvCzRbJhFjClFxClGxDtIbG`FbErMtLlIdJxDpEjDnEhEbGdD`FzDtGzYth@vh@j_AjGlKpL|SjDlGhDvFvDbHl]xm@|MjU`d@zw@nOnXdEvH~IrOt^ro@jXbe@~LzTfmAxvBhZjh@nj@baAxAnCvTh`@|Vnc@rT``@~Yfh@tPbZbk@|aAnH`MhQ|ZbX~d@vExGxGtHlCdCdIrGzD~BtDlBvDzArEpApE`AnQnChFh@lDn@~o@xN``@rKtOvBjDl@hRrCbCn@jDnAjDhBtDlCrAdApEjEnArAdCjD|BrDvAlClBdE|AxDno@vfBf@bApf@buApk@nbBzJ~YHT^hB|FzPpI|VlR|j@pJlYbHzRbu@vxBnYjz@`DtJxSvm@rA~Drc@|pANz@jVxs@tHtQrUxm@|gAdsCtS`i@jJjVf`@lbAfMt[~D~KbFzOhCjJzChMfAhF|B~JrC`NdPht@p@~CbF`U~ClOjEvR~BfJnDxLvB~GlCxHzTjp@nNfb@rFxPpHvT|AdEpBhGxGtR`F|NzAtEhPtf@zHzTfG`RtC|HlRxj@fMd_@`HhSzKv\\\\bB|EfAhD~FtP|DzItFtJlCtD~E|FnAlA~ClCnIdGn@l@h@d@nAz@dBbBtJhHhNlJfCjBtCdClEpE|FtGdEzEbEpEfJ`LvCdDlNjOtGjH`[|]dKjLdBrBpBnBhMrNdIrJdHxHtDxDlF`Gd_@rb@lRdTtAlAfNxNdBhBvFrGvDdEzBhCvBpBvAbA~BhAlA`@nBZnBHxAEzBW~Bu@xAs@lBuAbBaB~@kAfAgBz@aBn@yAp@kBj@qBz@cEd@aDTmCPqDToP\\\\qK|AwZVwDX{C`AoHhRokApAqGjAkFtAoEpAiDxJeTnAuBtCeFdEsFdDyC`CgBzJ{FtD}AhBc@jB[nA]vAm@dDqBt@w@xAmAlAaB~DkGh@iA`CaG|@oBlCcHAC"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 53.553637099999996,
                                  "longitude": 10.0066775
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 52.376760999999995,
                                  "longitude": 9.741021
                                }
                              },
                              "navigationInstruction": {
                                "instructions": "Hochgeschwindigkeitszug in Richtung Stuttgart Hbf"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "178 km"
                                },
                                "staticDuration": {
                                  "text": "1 Stunde, 19 Minuten"
                                }
                              },
                              "transitDetails": {
                                "stopDetails": {
                                  "arrivalStop": {
                                    "name": "Hannover Hauptbahnhof",
                                    "location": {
                                      "latLng": {
                                        "latitude": 52.376760999999995,
                                        "longitude": 9.741021
                                      }
                                    }
                                  },
                                  "arrivalTime": "2026-04-02T09:48:00Z",
                                  "departureStop": {
                                    "name": "Hamburg Hauptbahnhof",
                                    "location": {
                                      "latLng": {
                                        "latitude": 53.553637099999996,
                                        "longitude": 10.0066775
                                      }
                                    }
                                  },
                                  "departureTime": "2026-04-02T08:29:00Z"
                                },
                                "localizedValues": {
                                  "arrivalTime": {
                                    "time": {
                                      "text": "11:48"
                                    },
                                    "timeZone": "Europe/Berlin"
                                  },
                                  "departureTime": {
                                    "time": {
                                      "text": "10:29"
                                    },
                                    "timeZone": "Europe/Berlin"
                                  }
                                },
                                "headsign": "Stuttgart Hbf",
                                "transitLine": {
                                  "agencies": [
                                    {
                                      "name": "DB Fernverkehr AG",
                                      "uri": "https://www.bahn.de/"
                                    }
                                  ],
                                  "color": "#f01414",
                                  "nameShort": "ICE 579",
                                  "textColor": "#ffffff",
                                  "vehicle": {
                                    "name": {
                                      "text": "Hochgeschwindigkeitszug"
                                    },
                                    "type": "HIGH_SPEED_TRAIN",
                                    "iconUri": "//maps.gstatic.com/mapfiles/transit/iw2/6/rail2.png",
                                    "localIconUri": "//maps.gstatic.com/mapfiles/transit/iw2/6/de-db.png"
                                  }
                                },
                                "stopCount": 3
                              },
                              "travelMode": "TRANSIT"
                            },
                            {
                              "distanceMeters": 61644,
                              "staticDuration": "3000s",
                              "polyline": {
                                "encodedPolyline": "q}t~Hmvmz@dAvArF_O`B{D`BcFVmAl@_CRgA`@yCx@eLjAkQtAsV|AeYb@wLLiI@mIGkGa@oSAgC_Aof@KqHk@kZ}@a`@_Cm~@MuHGgJA{Q[i_@WcUWca@BiMDmGZgMXqGz@uL`@kEv@aH~@aHdKqx@pHyk@n@kFr@yHf@_JVmIBkIA_EY{PyBydAYiIo@{Kw@sIaAeIcXglBuD_XuAmMWeEa@_I_@gLUeKa@oLs@kWsAee@UcHMeHm@eUm@g[_@iMmAwj@MwPMaYGeIi@ePe@sQ[gIe@wFeAqIq@kGQmCImCAeCFaFLmCTcCJ_Aj@oDl@eCz@sCfAqCrBiDv@qA|@gAtHyHbB_CdByC~@kB`AiCp@aC`CuOdB_NhGce@jFs`@hBuNpEm]pNudALc@rKax@zFab@|AkMl@cG?e@t@oHh@yH|D__AfAyUBuAn@wMvIsqB~C_p@zDu~@fCan@jIyfBbDgs@|@eNlAkNr@oG~AyKdBkKjAiGrB}I~BeJrBwGlAsDvC}HlEmKzMa[hAqClXuo@|CoI|@kCnCwJ|BmJ|AiHbDgNjAsFzEwSRaAAAaB|HbMyj@tdA}tE|Hw]vCqMb@sBp@{DfA{HP}A`@{Ef@sLJaG?gDEgIKkI?nIScX@sHBKAAJ?`@kXXiXJoFb@oORuEfAoMnCyVx@oGtAiJ`CeMbE{QrDyMtB{IdA_FbNws@fDgPvAaGpCoJlFyOlHeTtAuDfAaC~CcGvz@kzAh_@}o@zmB}gDtDoGhl@scAzQ_\\\\fF_Jr@w@~B{ElDoGtXee@bZih@rQi[~W_e@`j@i`AnB_DjHwN|DwJlCcIjCgJvBwIt@cDl@cD~BqNtAoK~@iHx@cIf@sFx@sLLqCViIFmG?cFUuMc@yIk@yHi@}Fc@aGSeF{@kJc@_EoAmIcB_J__@whBcBeHyDiRwAmIwAcK}@qIo@eIm@gKYwHQqKCyHDaJPoJTaIpDc|@PuIBwEAeFOoJY}G[kFu@yI{C_[iAuImAiHaLel@oAsF_@yAmAsDoDwIyAqDiAgDy@aD]wAs@eEk@aF_@kFcAyYUaDc@yE}@{Fi@kCo@kCs@_CaAqC{AgD}AyC{@yAyByC[[eBaCkAsBg@q@aC_E}@iAiFeJAB"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 52.37737,
                                  "longitude": 9.74199
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 52.252219999999994,
                                  "longitude": 10.5403
                                }
                              },
                              "navigationInstruction": {
                                "instructions": "Zug oder S-Bahn in Richtung Braunschweig Hauptbahnhof"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "61,6 km"
                                },
                                "staticDuration": {
                                  "text": "50 Minuten"
                                }
                              },
                              "transitDetails": {
                                "stopDetails": {
                                  "arrivalStop": {
                                    "name": "Braunschweig Hauptbahnhof",
                                    "location": {
                                      "latLng": {
                                        "latitude": 52.252219999999994,
                                        "longitude": 10.5403
                                      }
                                    }
                                  },
                                  "arrivalTime": "2026-04-02T10:45:00Z",
                                  "departureStop": {
                                    "name": "Hannover Hauptbahnhof",
                                    "location": {
                                      "latLng": {
                                        "latitude": 52.37737,
                                        "longitude": 9.74199
                                      }
                                    }
                                  },
                                  "departureTime": "2026-04-02T09:55:00Z"
                                },
                                "localizedValues": {
                                  "arrivalTime": {
                                    "time": {
                                      "text": "12:45"
                                    },
                                    "timeZone": "Europe/Berlin"
                                  },
                                  "departureTime": {
                                    "time": {
                                      "text": "11:55"
                                    },
                                    "timeZone": "Europe/Berlin"
                                  }
                                },
                                "headsign": "Braunschweig Hauptbahnhof",
                                "transitLine": {
                                  "agencies": [
                                    {
                                      "name": "Westfalenbahn",
                                      "phoneNumber": "+49 521 55777755",
                                      "uri": "http://www.westfalenbahn.de/"
                                    }
                                  ],
                                  "name": "RE70",
                                  "color": "#ec6608",
                                  "textColor": "#000000",
                                  "vehicle": {
                                    "name": {
                                      "text": "Zug oder S-Bahn"
                                    },
                                    "type": "HEAVY_RAIL",
                                    "iconUri": "//maps.gstatic.com/mapfiles/transit/iw2/6/rail2.png"
                                  }
                                },
                                "stopCount": 7
                              },
                              "travelMode": "TRANSIT"
                            },
                            {
                              "distanceMeters": 24,
                              "staticDuration": "19s",
                              "polyline": {
                                "encodedPolyline": "st|}Hwii_A`@n@"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 52.2530596,
                                  "longitude": 10.5386809
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 52.2528947,
                                  "longitude": 10.538440999999999
                                }
                              },
                              "navigationInstruction": {
                                "maneuver": "DEPART",
                                "instructions": "Auf B248/B4 nach Südwesten"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "24 m"
                                },
                                "staticDuration": {
                                  "text": "1 Minute"
                                }
                              },
                              "travelMode": "WALK"
                            },
                            {
                              "distanceMeters": 61,
                              "staticDuration": "50s",
                              "polyline": {
                                "encodedPolyline": "qs|}Hghi_At@}AHa@"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 52.2528947,
                                  "longitude": 10.538440999999999
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 52.2525683,
                                  "longitude": 10.539078799999999
                                }
                              },
                              "navigationInstruction": {
                                "maneuver": "TURN_LEFT",
                                "instructions": "Links abbiegen"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "61 m"
                                },
                                "staticDuration": {
                                  "text": "1 Minute"
                                }
                              },
                              "travelMode": "WALK"
                            },
                            {
                              "distanceMeters": 23,
                              "staticDuration": "19s",
                              "polyline": {
                                "encodedPolyline": "qq|}Hgli_A]o@"
                              },
                              "startLocation": {
                                "latLng": {
                                  "latitude": 52.2525683,
                                  "longitude": 10.539078799999999
                                }
                              },
                              "endLocation": {
                                "latLng": {
                                  "latitude": 52.252716899999996,
                                  "longitude": 10.539322799999999
                                }
                              },
                              "navigationInstruction": {
                                "maneuver": "TURN_LEFT",
                                "instructions": "Links abbiegen"
                              },
                              "localizedValues": {
                                "distance": {
                                  "text": "23 m"
                                },
                                "staticDuration": {
                                  "text": "1 Minute"
                                }
                              },
                              "travelMode": "WALK"
                            }
                          ],
                          "localizedValues": {
                            "distance": {
                              "text": "240 km"
                            },
                            "duration": {
                              "text": "2 Stunden, 20 Minuten"
                            },
                            "staticDuration": {
                              "text": "2 Stunden, 20 Minuten"
                            }
                          },
                          "stepsOverview": {
                            "multiModalSegments": [
                              {
                                "stepStartIndex": 0,
                                "stepEndIndex": 4,
                                "navigationInstruction": {
                                  "instructions": "Gehen bis Hamburg Hauptbahnhof"
                                },
                                "travelMode": "WALK"
                              },
                              {
                                "stepStartIndex": 5,
                                "stepEndIndex": 5,
                                "navigationInstruction": {
                                  "instructions": "Hochgeschwindigkeitszug in Richtung Stuttgart Hbf"
                                },
                                "travelMode": "TRANSIT"
                              },
                              {
                                "stepStartIndex": 6,
                                "stepEndIndex": 6,
                                "navigationInstruction": {
                                  "instructions": "Zug oder S-Bahn in Richtung Braunschweig Hauptbahnhof"
                                },
                                "travelMode": "TRANSIT"
                              },
                              {
                                "stepStartIndex": 7,
                                "stepEndIndex": 9,
                                "travelMode": "WALK"
                              }
                            ]
                          }
                        }
                      ],
                      "distanceMeters": 240215,
                      "duration": "8411s",
                      "staticDuration": "8411s",
                      "polyline": {
                        "encodedPolyline": "gpzeIwda|@EIs@AI?IP@OEU?FmAgFEUf@W|J}DnAaAf@k@`AiBjAkDx@oDRmAZ_EPaDDaGCsASaDm@eE}@uFMkBEqCHoBPcB^aBr@eBt@kAjAgA|DmCdEeCpHuFzDcDfI_G~@g@lBq@nBY|AArBPvYnGtA\\\\x@VdCnArBzAhSpQpIvIzEfEnLbLrC|BpGpEvJ~FtD`ChB|@nJfDdHbD~DvAlPdF~Bl@nCd@`DVjDBzGObIDbLTvDV~Ex@xHxB~EdAxYzFb@FrDnAdDd@nNvAzQ~AlM|AhJ|AvKzBbG|Ah@VlN~DrN~EhTxIxRhI~DpApIjDhJrDjLxDnIjDdDnAzDnA~@TjC\\\\|@BpB@fD[pE}@hEiAnAg@vBiAbBoAfCgCdCeDp@eAXa@vT}_@jQ{YzF}JtD_GrAkB`D_EpD_ErH{GzEmErAwAfGqHdBeB~BqBdFgE~@y@tCcDxMgPdRcUtFsGxBwCpDuF|@mAtGgLf@w@d@cA^s@b@cAvDqGfIyOvAuC|BmFvAqDtEiMhHsUlEsMjF_NdF}KfBsDrByDxMoU~IqOzBqDhR_\\\\lEiInDsHpH{PxCqGzEeLjAyB`O}]~AoEv@kBbByEXo@hBmGj@mCZyCViEpA}Y|@sPnAkPzAiIlDqPxDqOhD{Ln@qBrFqOxDwJ`CsFnDsHvEyIrFkJ|DiGxIeMbG}JfFgJbDuGvJkThDqI~@gCpIoWhHgWn@_CrBoJlAsFtC_O`CiNdD}TxWekBxHui@b@qC~CaUf@wDpCiR`Gub@tFaf@rA{Jz@uF`AmF|C}OtBwJtB}HjAeE`K}_@fBiGvEaO|EuNzAyDrCcHbAgCxKmXdOy_@~FwNnOi`@xIuTfLoZbPca@pIqStWqp@jCsHfFwPjAkE~Hy\\\\`GmWnIc^|DkQlL}f@~I_`@n@yCdJo`@zG}Y`Jy_@tBiJvFsU~Nko@?]`CsJ`I_]zAsHlDsNnYcoA`i@}|B|DePzBoIjBiG`DaKpEsLdEwJfHgNtRq^fB_DrB}DbYkh@pI{OfG}KvHqNzGyL~Tsa@hA{BtA_CbDeGvA{BlA_BrBaCt@s@~@u@hGmDhCuA|EqBvBo@xE{@hAO`COnD?hHn@fI`BtCLjBNhTTrECzAExEs@nPoFx@c@|B}@bFyAdGkA|B[tDYdFSjJE|FBzJKfGYrDc@zFeAvOaErAg@dQsEfLuCdc@kLfHqB~YgHb]cJpPsEtHmCbDyAdFeCxAw@jCiBlFuDtDyCfDwCxHmHtAyAtGgGtEuD`EqChFyCtHeE~ScMjNcIhSmLlEoCtWqOhFyChAm@lHcElM{HlXsOtCqBfEkC`D_C~CgCtJiJzH{IpFqHjDgF`HiK`EyFnRqYhGoJtHsK`GsIdRcY~BuDpE_I~BcEjEwIhBwD|E_LtEcMfDgKlC_K|Lwe@hAsDbD_KxBcGjDqIrDgItDkHxEgIfFwHfe@iq@n_@}i@~Vo^vk@uy@pIsL~AcCpIcMrPyUrI_MtDyExDeElDgDns@kn@lIoHnf@gc@fH_GrEyCdEwBpDsAbA[vGyAtEi@`ESrDK`WInELvBPtH`AjVjFnYpGvF|@~Ab@x@J`Cx@zFlC~BxAxUpOpElC|JtEr}@v[jk@dSvEhBrErBlDvBzDtCpEjEzBfCvCpDjd@ln@tDlE`BfBpGdFlEnCbB|@bCdAzBx@dGzAxCf@dDZrEPnBA~EU|Di@rEgAzf@iOjQsFzImC~YgJpR_GrAe@pEcA~AS|DG~CVlB\\\\dBl@pB~@dBbAjGjEjL|IvAlA|B`CnDxE`FrIrUhe@v@zAd@x@zD`IdQz]zH~OhBbEzAbDrAdClIvPvGtMvLlVhb@zz@lBlDhFtKnJnRxFjL~GdN~GvP|@fCtBvFfH`TpOzg@dz@xsCrEhP^rAfCdIvEvOv@rChEnN|AnEbEhLvDpJ|F`NxDdIhDtGxDrGrD~FlH|Jfp@xy@vDzEjM~P|OrSvSjXlOvRfa@ph@lNrQrJrMbG~IfFhI~CvFbJjQtGhM~JbSxCzFxChG~Tdd@~FvLdNlXhDfIpDlJhDtJhFxPtCtKvBlJfErS`AvFbAlG`ChR`B|M|Gnm@rKf|@ZdC|@pGlBdLdA`GdGfXfF`RzCtJ|ClIxBtFzGlPlAjDdGbOtBvExBjErDrGhHvK~ErGzEzFb@^`HnHzD`E|eAveAjT~RzJtJbDrCpLhJ|K|GxObInmArl@nMdGxVzLzQ~IdCfAbHfD`GxCzB~@bKbF|L|FlGtChCtAtaAde@rVxLxFvCzRbJhFjClFxClGxDtIbG`FbErMtLlIdJxDpEjDnEhEbGdD`FzDtGzYth@vh@j_AjGlKpL|SjDlGhDvFvDbHl]xm@|MjU`d@zw@nOnXdEvH~IrOt^ro@jXbe@~LzTfmAxvBhZjh@nj@baAxAnCvTh`@|Vnc@rT``@~Yfh@tPbZbk@|aAnH`MhQ|ZbX~d@vExGxGtHlCdCdIrGzD~BtDlBvDzArEpApE`AnQnChFh@lDn@~o@xN``@rKtOvBjDl@hRrCbCn@jDnAjDhBtDlCrAdApEjEnArAdCjD|BrDvAlClBdE|AxDno@vfBf@bApf@buApk@nbBzJ~YHT^hB|FzPpI|VlR|j@pJlYbHzRbu@vxBnYjz@`DtJxSvm@rA~Drc@|pANz@jVxs@tHtQrUxm@|gAdsCtS`i@jJjVf`@lbAfMt[~D~KbFzOhCjJzChMfAhF|B~JrC`NdPht@p@~CbF`U~ClOjEvR~BfJnDxLvB~GlCxHzTjp@nNfb@rFxPpHvT|AdEpBhGxGtR`F|NzAtEhPtf@zHzTfG`RtC|HlRxj@fMd_@`HhSzKv\\\\bB|EfAhD~FtP|DzItFtJlCtD~E|FnAlA~ClCnIdGn@l@h@d@nAz@dBbBtJhHhNlJfCjBtCdClEpE|FtGdEzEbEpEfJ`LvCdDlNjOtGjH`[|]dKjLdBrBpBnBhMrNdIrJdHxHtDxDlF`Gd_@rb@lRdTtAlAfNxNdBhBvFrGvDdEzBhCvBpBvAbA~BhAlA`@nBZnBHxAEzBW~Bu@xAs@lBuAbBaB~@kAfAgBz@aBn@yAp@kBj@qBz@cEd@aDTmCPqDToP\\\\qK|AwZVwDX{C`AoHhRokApAqGjAkFtAoEpAiDxJeTnAuBtCeFdEsFdDyC`CgBzJ{FtD}AhBc@jB[nA]vAm@dDqBt@w@xAmAlAaB~DkGh@iA`CaG|@oBlCcHACyBaEdAvArF_O`B{D`BcFVmAl@_CRgA`@yCx@eLjAkQtAsV|AeYb@wLLiI@mIGkGa@oSAgC_Aof@KqHk@kZ}@a`@_Cm~@MuHGgJA{Q[i_@WcUWca@BiMDmGZgMXqGz@uL`@kEv@aH~@aHdKqx@pHyk@n@kFr@yHf@_JVmIBkIA_EY{PyBydAYiIo@{Kw@sIaAeIcXglBuD_XuAmMWeEa@_I_@gLUeKa@oLs@kWsAee@UcHMeHm@eUm@g[_@iMmAwj@MwPMaYGeIi@ePe@sQ[gIe@wFeAqIq@kGQmCImCAeCFaFLmCTcCJ_Aj@oDl@eCz@sCfAqCrBiDv@qA|@gAtHyHbB_CdByC~@kB`AiCp@aC`CuOdB_NhGce@jFs`@hBuNpEm]pNudALc@rKax@zFab@|AkMl@cG?e@t@oHh@yH|D__AfAyUBuAn@wMvIsqB~C_p@zDu~@fCan@jIyfBbDgs@|@eNlAkNr@oG~AyKdBkKjAiGrB}I~BeJrBwGlAsDvC}HlEmKzMa[hAqClXuo@|CoI|@kCnCwJ|BmJ|AiHbDgNjAsFzEwSRaAAAaB|HbMyj@tdA}tE|Hw]vCqMb@sBp@{DfA{HP}A`@{Ef@sLJaG?gDEgIKkI?nIScX@sHBKAAJ?`@kXXiXJoFb@oORuEfAoMnCyVx@oGtAiJ`CeMbE{QrDyMtB{IdA_FbNws@fDgPvAaGpCoJlFyOlHeTtAuDfAaC~CcGvz@kzAh_@}o@zmB}gDtDoGhl@scAzQ_\\\\fF_Jr@w@~B{ElDoGtXee@bZih@rQi[~W_e@`j@i`AnB_DjHwN|DwJlCcIjCgJvBwIt@cDl@cD~BqNtAoK~@iHx@cIf@sFx@sLLqCViIFmG?cFUuMc@yIk@yHi@}Fc@aGSeF{@kJc@_EoAmIcB_J__@whBcBeHyDiRwAmIwAcK}@qIo@eIm@gKYwHQqKCyHDaJPoJTaIpDc|@PuIBwEAeFOoJY}G[kFu@yI{C_[iAuImAiHaLel@oAsF_@yAmAsDoDwIyAqDiAgDy@aD]wAs@eEk@aF_@kFcAyYUaDc@yE}@{Fi@kCo@kCs@_CaAqC{AgD}AyC{@yAyByC[[eBaCkAsBg@q@aC_E}@iAiFeJABgDbI`@n@t@}AHa@]o@"
                      },
                      "viewport": {
                        "low": {
                          "latitude": 52.227435,
                          "longitude": 9.6870915
                        },
                        "high": {
                          "latitude": 53.5536699,
                          "longitude": 10.5822457
                        }
                      },
                      "travelAdvisory": {
                        "transitFare": {}
                      },
                      "localizedValues": {
                        "distance": {
                          "text": "240 km"
                        },
                        "duration": {
                          "text": "2 Stunden, 20 Minuten"
                        },
                        "staticDuration": {
                          "text": "2 Stunden, 20 Minuten"
                        },
                        "transitFare": {}
                      },
                      "routeLabels": [
                        "DEFAULT_ROUTE"
                      ]
                    }
                  ],
                  "geocodingResults": {
                    "origin": {
                      "geocoderStatus": {},
                      "type": [
                        "establishment",
                        "point_of_interest",
                        "train_station",
                        "transit_station"
                      ],
                      "placeId": "ChIJ89cMROGOsUcRJGABermAXEk"
                    },
                    "destination": {
                      "geocoderStatus": {},
                      "type": [
                        "establishment",
                        "point_of_interest",
                        "transit_station"
                      ],
                      "placeId": "ChIJTwV3Mej1r0cRJAd5ngNeAlQ"
                    }
                  }
                }
            """;

}
