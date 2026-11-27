import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ObjectifStrategique, ObjectifStrategiqueRequestDto } from '../models/budget/objectif-strategique.model';
import { ObjectifOperationnel, ObjectifOperationnelRequestDto } from '../models/budget/objectif-operationnel.model';
import { ActiviteCatalogue, ActiviteCatalogueRequestDto } from '../models/budget/activite-catalogue.model';

@Injectable({ providedIn: 'root' })
export class NomenclatureService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listerOs(): Observable<ObjectifStrategique[]> { return this.http.get<ObjectifStrategique[]>(`${this.api}/objectifs-strategiques`); }
  creerOs(dto: ObjectifStrategiqueRequestDto): Observable<ObjectifStrategique> { return this.http.post<ObjectifStrategique>(`${this.api}/objectifs-strategiques`, dto); }
  modifierOs(id: string, dto: ObjectifStrategiqueRequestDto): Observable<ObjectifStrategique> { return this.http.put<ObjectifStrategique>(`${this.api}/objectifs-strategiques/${id}`, dto); }
  supprimerOs(id: string): Observable<void> { return this.http.delete<void>(`${this.api}/objectifs-strategiques/${id}`); }

  listerOp(osId?: string): Observable<ObjectifOperationnel[]> {
    const params = osId ? new HttpParams().set('osId', osId) : new HttpParams();
    return this.http.get<ObjectifOperationnel[]>(`${this.api}/objectifs-operationnels`, { params });
  }
  creerOp(dto: ObjectifOperationnelRequestDto): Observable<ObjectifOperationnel> { return this.http.post<ObjectifOperationnel>(`${this.api}/objectifs-operationnels`, dto); }
  modifierOp(id: string, dto: ObjectifOperationnelRequestDto): Observable<ObjectifOperationnel> { return this.http.put<ObjectifOperationnel>(`${this.api}/objectifs-operationnels/${id}`, dto); }
  supprimerOp(id: string): Observable<void> { return this.http.delete<void>(`${this.api}/objectifs-operationnels/${id}`); }

  listerActivites(opId?: string): Observable<ActiviteCatalogue[]> {
    const params = opId ? new HttpParams().set('opId', opId) : new HttpParams();
    return this.http.get<ActiviteCatalogue[]>(`${this.api}/activites-catalogue`, { params });
  }
  creerActivite(dto: ActiviteCatalogueRequestDto): Observable<ActiviteCatalogue> { return this.http.post<ActiviteCatalogue>(`${this.api}/activites-catalogue`, dto); }
  modifierActivite(id: string, dto: ActiviteCatalogueRequestDto): Observable<ActiviteCatalogue> { return this.http.put<ActiviteCatalogue>(`${this.api}/activites-catalogue/${id}`, dto); }
  supprimerActivite(id: string): Observable<void> { return this.http.delete<void>(`${this.api}/activites-catalogue/${id}`); }
}
