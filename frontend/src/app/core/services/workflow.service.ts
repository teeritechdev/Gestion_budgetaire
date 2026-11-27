import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page-response.model';
import {
  WorkflowDefinition,
  WorkflowDefinitionRequest,
  WorkflowEtape,
  WorkflowInstance,
  WorkflowDecisionRequest,
  WorkflowHistorique
} from '../models/workflow/workflow.model';

export type ListParams = Record<string, string | number | boolean | null | undefined>;

function toHttpParams(params: ListParams): HttpParams {
  let httpParams = new HttpParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  }
  return httpParams;
}

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // --- Modèles de workflows (Définitions) ---
  listerDefinitions(params: ListParams): Observable<PageResponse<WorkflowDefinition>> {
    return this.http.get<PageResponse<WorkflowDefinition>>(`${this.api}/workflows`, {
      params: toHttpParams(params)
    });
  }

  obtenirDefinition(id: string): Observable<WorkflowDefinition> {
    return this.http.get<WorkflowDefinition>(`${this.api}/workflows/${id}`);
  }

  creerDefinition(dto: WorkflowDefinitionRequest): Observable<WorkflowDefinition> {
    return this.http.post<WorkflowDefinition>(`${this.api}/workflows`, dto);
  }

  modifierDefinition(id: string, dto: WorkflowDefinitionRequest): Observable<WorkflowDefinition> {
    return this.http.put<WorkflowDefinition>(`${this.api}/workflows/${id}`, dto);
  }

  supprimerDefinition(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/workflows/${id}`);
  }

  // --- Configuration des étapes ---
  listerEtapes(workflowId: string): Observable<WorkflowEtape[]> {
    return this.http.get<WorkflowEtape[]>(`${this.api}/workflows/${workflowId}/etapes`);
  }

  enregistrerEtape(workflowId: string, dto: WorkflowEtape): Observable<WorkflowEtape> {
    return this.http.post<WorkflowEtape>(`${this.api}/workflows/${workflowId}/etapes`, dto);
  }

  supprimerEtape(workflowId: string, etapeId: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/workflows/${workflowId}/etapes/${etapeId}`);
  }

  // --- Instances & Suivi ---
  listerInstances(params: ListParams): Observable<PageResponse<WorkflowInstance>> {
    return this.http.get<PageResponse<WorkflowInstance>>(`${this.api}/workflows/instances`, {
      params: toHttpParams(params)
    });
  }

  obtenirInstance(id: string): Observable<WorkflowInstance> {
    return this.http.get<WorkflowInstance>(`${this.api}/workflows/instances/${id}`);
  }

  obtenirInstanceParDossier(dossierId: string): Observable<WorkflowInstance> {
    return this.http.get<WorkflowInstance>(`${this.api}/workflows/instances/dossier/${dossierId}`);
  }

  obtenirHistorique(instanceId: string): Observable<WorkflowHistorique[]> {
    return this.http.get<WorkflowHistorique[]>(`${this.api}/workflows/instances/${instanceId}/historique`);
  }

  traiterDecision(instanceId: string, dto: WorkflowDecisionRequest): Observable<WorkflowInstance> {
    return this.http.post<WorkflowInstance>(`${this.api}/workflows/instances/${instanceId}/decision`, dto);
  }

  mesValidations(): Observable<WorkflowInstance[]> {
    return this.http.get<WorkflowInstance[]>(`${this.api}/workflows/mes-validations`);
  }
}

