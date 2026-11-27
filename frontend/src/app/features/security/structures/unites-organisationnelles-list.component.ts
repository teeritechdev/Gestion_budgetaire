import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { UniteOrganisationnelleService } from '../../../core/services/unite-organisationnelle.service';
import { StructureService } from '../../../core/services/structure.service';
import { UtilisateurService } from '../../../core/services/utilisateur.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { of } from 'rxjs';
import { UniteOrganisationnelle, TypeStructure } from '../../../core/models/unite-organisationnelle.model';
import { Structure } from '../../../core/models/structure.model';
import { Utilisateur } from '../../../core/models/utilisateur.model';

@Component({
  selector: 'app-structures-list',
  standalone: true,
  imports: [RecordDetailsDialogComponent, CommonModule, FormsModule],
  template: `
    <div class="space-y-6 max-w-7xl mx-auto">
      <!-- En-tête de la page -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold text-white tracking-tight">Unités organisationnelles</h1>
          <p class="text-sm text-slate-600 mt-1">
            Gestion de l'organigramme de l'entreprise : Directions, Départements, Services et Agences.
          </p>
        </div>

        <div class="action-toolbar flex items-center gap-2.5">
          <button
            [hidden]="!auth.hasPermission('structure.exporter')"
            (click)="exportCsv()"
            class="px-3.5 py-2 rounded-xl bg-blue-700 hover:bg-blue-800 text-white text-xs font-medium border border-blue-800 transition-colors flex items-center gap-1.5">
            <svg class="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Export CSV
          </button>

          <button
            [hidden]="!auth.hasPermission('structure.exporter')"
            (click)="exportPdf()"
            class="px-3.5 py-2 rounded-xl bg-blue-700 hover:bg-blue-800 text-white text-xs font-medium border border-blue-800 transition-colors flex items-center gap-1.5">
            <svg class="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
            Export PDF
          </button>

          <button
            [hidden]="!auth.hasPermission('structure.creer')"
            (click)="openCreateModal()"
            class="px-4 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium shadow-lg shadow-blue-600/30 transition-all flex items-center gap-1.5">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            Nouvelle Structure
          </button>
        </div>
      </div>

      <!-- Filtres et Mode d'affichage -->
      <div class="bg-white border border-slate-200 rounded-2xl p-4 shadow-lg flex flex-wrap items-center justify-between gap-3">
        <div class="flex flex-wrap items-center gap-3 flex-1">
          <!-- Recherche par libellé -->
          <div class="min-w-[200px]">
            <input
              type="text"
              [(ngModel)]="searchLibelle"
              (input)="applyFilters()"
              placeholder="Rechercher par libellé ou code..."
              class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <!-- Filtre Entité -->
          <select
            [(ngModel)]="selectedEntiteCode"
            (change)="applyFilters()"
            class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
            <option [ngValue]="null">Toutes les Entités</option>
            @for (e of entites(); track e.code) {
              <option [value]="e.code">{{ e.nom }} ({{ e.code }})</option>
            }
          </select>

          <!-- Filtre Type Structure -->
          <select
            [(ngModel)]="selectedTypeStructure"
            (change)="applyFilters()"
            class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
            <option [ngValue]="null">Tous les Types</option>
            <option value="DIRECTION">DIRECTION</option>
            <option value="DEPARTEMENT">DEPARTEMENT</option>
            <option value="SERVICE">SERVICE</option>
            <option value="AGENCE">AGENCE</option>
          </select>

          <!-- Filtre Actif -->
          <select
            [(ngModel)]="selectedActif"
            (change)="applyFilters()"
            class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
            <option [ngValue]="null">Tous statuts</option>
            <option [ngValue]="true">Actif</option>
            <option [ngValue]="false">Inactif</option>
          </select>
        </div>

        <!-- Bascule Vue Tableau / Cartes -->
        <div class="flex items-center gap-1 bg-slate-50 p-1 rounded-xl border border-slate-200">
          <button
            (click)="viewMode.set('table')"
            [class.bg-slate-100]="viewMode() === 'table'"
            [class.text-white]="viewMode() === 'table'"
            [class.text-slate-600]="viewMode() !== 'table'"
            class="px-2.5 py-1 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5">
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
            </svg>
            Tableau
          </button>
          <button
            (click)="viewMode.set('cards')"
            [class.bg-slate-100]="viewMode() === 'cards'"
            [class.text-white]="viewMode() === 'cards'"
            [class.text-slate-600]="viewMode() !== 'cards'"
            class="px-2.5 py-1 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5">
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM14 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1V5zM4 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1v-4zM14 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z" />
            </svg>
            Cartes
          </button>
        </div>
      </div>

      <!-- Mode Tableau -->
      @if (viewMode() === 'table') {
        <div class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xl">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-50/80 text-slate-600 uppercase tracking-wider border-b border-slate-200">
              <tr>
                <th class="py-3.5 px-4">Code</th>
                <th class="py-3.5 px-4">Libellé</th>
                <th class="py-3.5 px-4">Type</th>
                <th class="py-3.5 px-4">Entité</th>
                <th class="py-3.5 px-4">Structure Parente</th>
                <th class="py-3.5 px-4">Direction Rattachement</th>
                <th class="py-3.5 px-4">Responsable</th>
                <th class="py-3.5 px-4">Statut</th>
                <th class="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200/60">
              @for (struct of structures(); track struct.id) {
                <tr class="hover:bg-slate-100/40 transition-colors">
                  <td class="py-3.5 px-4 font-mono font-medium text-blue-300">{{ struct.code }}</td>
                  <td class="py-3.5 px-4 font-semibold text-white">{{ struct.libelle }}</td>
                  <td class="py-3.5 px-4">
                    <span
                      class="px-2 py-0.5 rounded-full text-[10px] font-semibold border"
                      [ngClass]="{
                        'bg-blue-50 text-blue-800 border-blue-200': ['DIRECTION', 'DEPARTEMENT', 'SERVICE'].includes(struct.typeStructure),
                        'bg-blue-100 text-blue-800 border-blue-200': struct.typeStructure === 'AGENCE'
                      }">
                      {{ struct.typeStructure }}
                    </span>
                  </td>
                  <td class="py-3.5 px-4 text-slate-700">
                    {{ struct.structure?.nom || struct.structure?.code || '—' }}
                  </td>
                  <td class="py-3.5 px-4 text-slate-600">
                    {{ getStructureNameById(struct.parentId || struct.parent?.id) || '— (Racine)' }}
                  </td>
                  <td class="py-3.5 px-4 text-slate-600">
                    {{ getStructureNameById(struct.directionId || struct.direction?.id) || '—' }}
                  </td>
                  <td class="py-3.5 px-4 text-slate-700">
                    {{ getResponsableName(struct.responsableId) || 'Non désigné' }}
                  </td>
                  <td class="py-3.5 px-4">
                    <span
                      class="px-2 py-0.5 rounded-full text-[11px] font-medium inline-flex items-center gap-1"
                      [ngClass]="struct.actif ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'">
                      <span class="w-1.5 h-1.5 rounded-full" [ngClass]="struct.actif ? 'bg-blue-400' : 'bg-rose-400'"></span>
                      {{ struct.actif ? 'Actif' : 'Inactif' }}
                    </span>
                  </td>
                  <td class="py-3.5 px-4 text-right">
                    <div class="flex items-center justify-end gap-1.5">
                      <button type="button" (click)="showDetails('Structure', struct)" class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-blue-700 transition-colors hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2" title="Voir les détails" aria-label="Voir les détails"><svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2.25 12s3.5-6.5 9.75-6.5 9.75 6.5 9.75 6.5-3.5 6.5-9.75 6.5S2.25 12 2.25 12Z"/><circle cx="12" cy="12" r="3"/></svg></button><button
                        [hidden]="!auth.hasPermission('structure.modifier')"
                        (click)="openEditModal(struct)"
                        class="p-1.5 rounded-lg text-slate-600 hover:text-blue-400 hover:bg-green-600/10 transition-colors"
                        title="Modifier">
                        <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                      </button>
                      <button
                        [hidden]="!auth.hasPermission('structure.supprimer')"
                        (click)="confirmDelete(struct)"
                        class="p-1.5 rounded-lg text-rose-600 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                        title="Supprimer">
                        <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      <!-- Mode Cartes -->
      @if (viewMode() === 'cards') {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          @for (s of structures(); track s.id) {
            <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-lg flex flex-col justify-between hover:border-slate-300 transition-all">
              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <span class="font-mono text-xs px-2 py-0.5 rounded bg-blue-500/10 text-blue-300 font-bold border border-blue-500/20">
                    {{ s.code }}
                  </span>
                  <span
                    class="px-2 py-0.5 rounded-full text-[10px] font-semibold"
                    [ngClass]="{
                      'bg-blue-50 text-blue-800 border-blue-200': ['DIRECTION', 'DEPARTEMENT', 'SERVICE'].includes(s.typeStructure),
                      'bg-blue-100 text-blue-800 border-blue-200': s.typeStructure === 'AGENCE'
                    }">
                    {{ s.typeStructure }}
                  </span>
                </div>

                <div>
                  <h3 class="text-base font-bold text-white">{{ s.libelle }}</h3>
                  <p class="text-xs text-slate-600 mt-0.5">Structure : {{ s.structure?.nom || s.structure?.code }}</p>
                </div>

                <div class="p-3 bg-white rounded-xl border border-slate-200/80 space-y-1.5 text-[11px]">
                  <div class="flex justify-between text-slate-600">
                    <span>Parent (N-1) :</span>
                    <span class="text-slate-800 font-medium truncate max-w-[150px]">{{ getStructureNameById(s.parentId || s.parent?.id) || 'Racine' }}</span>
                  </div>
                  <div class="flex justify-between text-slate-600">
                    <span>Direction tutelle :</span>
                    <span class="text-slate-800 font-medium truncate max-w-[150px]">{{ getStructureNameById(s.directionId || s.direction?.id) || '—' }}</span>
                  </div>
                  <div class="flex justify-between text-slate-600">
                    <span>Responsable :</span>
                    <span class="text-blue-300 font-medium truncate max-w-[150px]">{{ getResponsableName(s.responsableId) || 'Non désigné' }}</span>
                  </div>
                </div>
              </div>

              <div class="mt-4 pt-3 border-t border-slate-200 flex items-center justify-between">
                <span class="text-xs" [class.text-blue-400]="s.actif" [class.text-rose-400]="!s.actif">
                  ● {{ s.actif ? 'Actif' : 'Inactif' }}
                </span>
                <div class="flex flex-wrap items-center gap-2">
                  <button type="button" (click)="showDetails('Structure', s)" class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-blue-700 transition-colors hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2" title="Voir les détails" aria-label="Voir les détails"><svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2.25 12s3.5-6.5 9.75-6.5 9.75 6.5 9.75 6.5-3.5 6.5-9.75 6.5S2.25 12 2.25 12Z"/><circle cx="12" cy="12" r="3"/></svg></button><button (click)="openEditModal(s)" class="inline-flex h-10 w-10 items-center justify-center rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 text-slate-600 hover:bg-green-50 hover:text-green-700 focus-visible:ring-green-600" title="Modifier" aria-label="Modifier"><svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg></button>
                  <span class="text-slate-600">|</span>
                  <button [hidden]="!auth.hasPermission('structure.supprimer')" (click)="confirmDelete(s)" class="inline-flex h-10 w-10 items-center justify-center rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 text-rose-600 hover:bg-rose-50 hover:text-rose-700 focus-visible:ring-rose-600" title="Supprimer" aria-label="Supprimer"><svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4h8v2m2 0-.8 14H6.8L6 6m4 4v6m4-6v6"/></svg></button>
                </div>
              </div>
            </div>
          }
        </div>
      }

      <!-- MODAL DE CRÉATION / MODIFICATION PROPRE -->
      @if (showModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div class="bg-white border border-slate-200 rounded-2xl w-full max-w-2xl p-6 shadow-2xl space-y-5 max-h-[92vh] overflow-y-auto">

            <!-- En-tête Modal Nettoyé -->
            <div class="flex items-center justify-between border-b border-slate-200 pb-3">
              <div>
                <h3 class="text-lg font-semibold text-white">
                  {{ isEditing() ? 'Modifier une unité organisationnelle' : 'Créer une unité organisationnelle' }}
                </h3>
                <p class="text-xs text-slate-600">
                  Renseignez les informations de l’unité et ses rattachements hiérarchiques.
                </p>
              </div>
              <button (click)="closeModal()" class="text-slate-600 hover:text-white">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <form (ngSubmit)="saveStructure()" class="space-y-4">
              <!-- Ligne 1 : Type de Structure & Code -->
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1.5">
                    Type d’unité *
                  </label>
                  <select
                    required
                    [(ngModel)]="formData.typeStructure"
                    (change)="onTypeStructureChange()"
                    name="typeStructure"
                    class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500">
                    <option value="DIRECTION">DIRECTION</option>
                    <option value="DEPARTEMENT">DEPARTEMENT</option>
                    <option value="SERVICE">SERVICE</option>
                    <option value="AGENCE">AGENCE</option>
                  </select>
                </div>

                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1.5">
                    Code unité *
                  </label>
                  <input
                    type="text"
                    required
                    maxlength="50"
                    [(ngModel)]="formData.code"
                    name="code"
                    class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white uppercase focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                  <p class="mt-1 text-[11px] text-slate-500">Code unique, ex. DIR-IT-01.</p>
                </div>
              </div>

              <!-- Ligne 2 : Libellé -->
              <div>
                <label class="block text-xs font-medium text-slate-700 mb-1.5">
                  Libellé / Désignation *
                </label>
                <input
                  type="text"
                  required
                  maxlength="150"
                  [(ngModel)]="formData.libelle"
                  name="libelle"
                  class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <!-- Ligne 3 : Entité Juridique -->
              <div>
                <label class="block text-xs font-medium text-slate-700 mb-1.5">
                  Entité Juridique de Rattachement *
                </label>
                <select
                  required
                  [(ngModel)]="selectedModalEntiteCode"
                  (change)="onEntiteChange()"
                  name="entiteCode"
                  class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500">
                  @for (e of entites(); track e.code) {
                    <option [value]="e.code">
                      {{ e.nom }} ({{ e.code }})
                    </option>
                  }
                </select>
              </div>

              <!-- Ligne 4 : Hiérarchie -->
              <div class="p-3.5 bg-slate-50/60 rounded-xl border border-slate-200 space-y-3">
                <div class="flex items-center gap-2">
                  <svg class="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                  <span class="text-xs font-semibold text-slate-800 uppercase tracking-wider">Hiérarchie & Rattachements</span>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <!-- Structure Parente -->
                  <div>
                    <label class="block text-xs font-medium text-slate-700 mb-1">
                      Structure Parente (N-1)
                    </label>
                    <select
                      [(ngModel)]="selectedParentId"
                      name="parentId"
                      class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500">
                      <option [ngValue]="null">Aucune (Niveau racine)</option>
                      @for (p of availableParents(); track p.id) {
                        <option [value]="p.id">
                          {{ p.libelle }} ({{ p.typeStructure }} - {{ p.code }})
                        </option>
                      }
                    </select>
                  </div>

                  <!-- Direction de Rattachement -->
                  <div>
                    <label class="block text-xs font-medium text-slate-700 mb-1">
                      Direction de Rattachement
                    </label>
                    <select
                      [(ngModel)]="selectedDirectionId"
                      name="directionId"
                      class="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500">
                      <option [ngValue]="null">Aucune / Est elle-même une Direction</option>
                      @for (d of availableDirections(); track d.id) {
                        <option [value]="d.id">
                          {{ d.libelle }} ({{ d.code }})
                        </option>
                      }
                    </select>
                  </div>
                </div>
              </div>

              <!-- Ligne 5 : Responsable de la structure -->
              <div>
                <label class="block text-xs font-medium text-slate-700 mb-1.5">
                  Responsable de la Structure
                </label>
                <select
                  [(ngModel)]="selectedResponsableId"
                  name="responsableId"
                  class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500">
                  <option [ngValue]="null">Aucun responsable assigné</option>
                  @for (u of utilisateurs(); track u.id) {
                    <option [value]="u.id">
                      {{ u.prenom }} {{ u.nom }} - {{ u.email }} {{ u.poste ? '(' + u.poste + ')' : '' }}
                    </option>
                  }
                </select>
              </div>

              <!-- Ligne 6 : Statut Actif -->
              <div class="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="structActif"
                  [(ngModel)]="formData.actif"
                  name="actif"
                  class="w-4 h-4 rounded text-blue-600 bg-slate-50 border-slate-300 focus:ring-blue-500"
                />
                <label for="structActif" class="text-xs text-slate-700 cursor-pointer">
                  Unité active (visible dans les allocations et affectations)
                </label>
              </div>

              <!-- Boutons d'action -->
              <div class="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
                <button
                  type="button"
                  (click)="closeModal()"
                  class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium transition-colors">
                  Annuler
                </button>
                <button
                  type="submit"
                  [disabled]="saving()"
                  class="px-5 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium shadow-lg shadow-blue-600/30 disabled:opacity-50 flex items-center gap-2 transition-all">
                  @if (saving()) {
                    <svg class="animate-spin h-3.5 w-3.5" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                  }
                  {{ isEditing() ? 'Enregistrer les Modifications' : 'Créer la Structure' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Modal de Confirmation de Suppression -->
      @if (structureToDelete()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div class="bg-white border border-slate-200 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <h3 class="text-lg font-semibold text-rose-400">Supprimer l’unité organisationnelle</h3>
            <p class="text-xs text-slate-700 leading-relaxed">
              Confirmez-vous la suppression de <strong class="text-white">{{ structureToDelete()?.libelle }}</strong> ({{ structureToDelete()?.code }}) ?
              Attention : les structures enfants ou utilisateurs rattachés bloqueront la suppression.
            </p>
            <div class="flex items-center justify-end gap-3 pt-3">
              <button
                (click)="structureToDelete.set(null)"
                class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium">
                Annuler
              </button>
              <button
                (click)="executeDelete()"
                class="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-medium">
                Supprimer Définitivement
              </button>
            </div>
          </div>
        </div>
      }
    </div>
    @if (detailRecord) { <app-record-details-dialog [title]="detailTitle" [record]="detailRecord" (close)="detailRecord = null" /> }
  `
})
export class UnitesOrganisationnellesListComponent implements OnInit {
  detailTitle = '';
  detailRecord: object | null = null;
  showDetails(title: string, record: object): void { this.detailTitle = title; this.detailRecord = record; }

  private readonly structureService = inject(UniteOrganisationnelleService);
  private readonly entiteService = inject(StructureService);
  private readonly utilisateurService = inject(UtilisateurService);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  structures = signal<UniteOrganisationnelle[]>([]);
  entites = signal<Structure[]>([]);
  utilisateurs = signal<Utilisateur[]>([]);
  viewMode = signal<'table' | 'cards'>('table');

  // Filtres
  searchLibelle = '';
  selectedEntiteCode: string | null = null;
  selectedTypeStructure: string | null = null;
  selectedActif: boolean | null = null;

  // Modal
  showModal = signal(false);
  isEditing = signal(false);
  saving = signal(false);
  currentId: string | null = null;

  // Modèle du formulaire
  formData: Partial<UniteOrganisationnelle> = {
    code: '',
    libelle: '',
    typeStructure: 'DIRECTION',
    actif: true
  };
  selectedModalEntiteCode = '';
  selectedParentId: string | null = null;
  selectedDirectionId: string | null = null;
  selectedResponsableId: string | null = null;

  // Suppression
  structureToDelete = signal<UniteOrganisationnelle | null>(null);

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    forkJoin({
      structures: this.structureService.listerOuFiltrer(),
      entites: this.auth.hasPermission('entite.lire')
        ? this.entiteService.listerOuFiltrer()
        : of([] as Structure[]),
      utilisateurs: this.auth.hasPermission('utilisateur.lire')
        ? this.utilisateurService.listerOuFiltrer()
        : of([] as Utilisateur[])
    }).subscribe({
      next: (res) => {
        this.structures.set(res.structures);
        this.entites.set(res.entites);
        this.utilisateurs.set(res.utilisateurs);
      },
      error: () => this.notify.error('Erreur chargement des données de structure')
    });
  }

  applyFilters() {
    this.structureService.listerOuFiltrer(
      this.selectedEntiteCode || undefined,
      this.selectedTypeStructure || undefined,
      this.selectedActif !== null ? this.selectedActif : undefined,
      this.searchLibelle || undefined
    ).subscribe({
      next: (res) => this.structures.set(res),
      error: () => {}
    });
  }

  availableParents(): UniteOrganisationnelle[] {
    return this.structures().filter(s => {
      if (this.isEditing() && s.id === this.currentId) return false;
      if (this.selectedModalEntiteCode && s.structure?.code !== this.selectedModalEntiteCode) return false;
      return true;
    });
  }

  availableDirections(): UniteOrganisationnelle[] {
    return this.structures().filter(s => {
      if (s.typeStructure !== 'DIRECTION') return false;
      if (this.isEditing() && s.id === this.currentId) return false;
      if (this.selectedModalEntiteCode && s.structure?.code !== this.selectedModalEntiteCode) return false;
      return true;
    });
  }

  getStructureNameById(id?: string | null): string | null {
    if (!id) return null;
    const s = this.structures().find(item => item.id === id);
    return s ? `${s.libelle} (${s.code})` : null;
  }

  getResponsableName(responsableId?: string | null): string | null {
    if (!responsableId) return null;
    const u = this.utilisateurs().find(user => user.id === responsableId);
    return u ? `${u.prenom} ${u.nom}` : null;
  }

  onTypeStructureChange() {
    if (this.formData.typeStructure === 'DIRECTION') {
      this.selectedDirectionId = null;
    }
  }

  onEntiteChange() {
    this.selectedParentId = null;
    this.selectedDirectionId = null;
  }

  openCreateModal() {
    this.isEditing.set(false);
    this.currentId = null;
    this.formData = {
      code: `DIR-${Math.floor(10 + Math.random() * 90)}`,
      libelle: '',
      typeStructure: 'DIRECTION',
      actif: true
    };
    this.selectedModalEntiteCode = this.entites()[0]?.code || '';
    this.selectedParentId = null;
    this.selectedDirectionId = null;
    this.selectedResponsableId = null;
    this.showModal.set(true);
  }

  openEditModal(s: UniteOrganisationnelle) {
    this.isEditing.set(true);
    this.currentId = s.id || null;
    this.formData = {
      code: s.code,
      libelle: s.libelle,
      typeStructure: s.typeStructure,
      actif: s.actif
    };
    this.selectedModalEntiteCode = s.structure?.code || this.entites()[0]?.code || '';
    this.selectedParentId = s.parentId || s.parent?.id || null;
    this.selectedDirectionId = s.directionId || s.direction?.id || null;
    this.selectedResponsableId = s.responsableId || null;
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
  }

  saveStructure() {
    const entite = this.entites().find(e => e.code === this.selectedModalEntiteCode);
    if (!entite) {
      this.notify.warning('Veuillez sélectionner une entité juridique.');
      return;
    }

    if (!this.formData.code || !this.formData.libelle) {
      this.notify.warning('Le code et le libellé sont obligatoires.');
      return;
    }

    this.saving.set(true);

    const parentObj = this.selectedParentId ? { id: this.selectedParentId } as UniteOrganisationnelle : null;
    const directionObj = this.selectedDirectionId ? { id: this.selectedDirectionId } as UniteOrganisationnelle : null;

    const payload: Partial<UniteOrganisationnelle> = {
      code: this.formData.code,
      libelle: this.formData.libelle,
      typeStructure: this.formData.typeStructure,
      structure: entite,
      parent: parentObj,
      direction: directionObj,
      responsableId: this.selectedResponsableId || null,
      actif: this.formData.actif ?? true
    };

    if (this.isEditing() && this.currentId) {
      this.structureService.modifier(this.currentId, payload).subscribe({
        next: () => {
          this.notify.success('Structure mise à jour avec succès');
          this.saving.set(false);
          this.closeModal();
          this.loadData();
        },
        error: (err) => {
          this.notify.error(err.error?.message || 'Erreur lors de la mise à jour de la structure');
          this.saving.set(false);
        }
      });
    } else {
      this.structureService.creer(payload).subscribe({
        next: () => {
          this.notify.success('Structure créée avec succès');
          this.saving.set(false);
          this.closeModal();
          this.loadData();
        },
        error: (err) => {
          this.notify.error(err.error?.message || 'Erreur lors de la création de la structure');
          this.saving.set(false);
        }
      });
    }
  }

  confirmDelete(s: UniteOrganisationnelle) {
    this.structureToDelete.set(s);
  }

  executeDelete() {
    const s = this.structureToDelete();
    if (!s || !s.id) return;

    this.structureService.supprimer(s.id).subscribe({
      next: () => {
        this.notify.success('Unité organisationnelle supprimée avec succès');
        this.structureToDelete.set(null);
        this.loadData();
      },
      error: (err) => {
        this.notify.error(err.error?.message || 'Impossible de supprimer cette unité organisationnelle');
      }
    });
  }

  exportCsv() {
    this.structureService.exporterCsv(
      this.selectedEntiteCode || undefined,
      this.selectedTypeStructure || undefined,
      this.selectedActif !== null ? this.selectedActif : undefined,
      this.searchLibelle || undefined
    ).subscribe(blob => this.downloadFile(blob, 'structures_export.csv'));
  }

  exportPdf() {
    this.structureService.exporterPdf(
      this.selectedEntiteCode || undefined,
      this.selectedTypeStructure || undefined,
      this.selectedActif !== null ? this.selectedActif : undefined,
      this.searchLibelle || undefined
    ).subscribe(blob => this.downloadFile(blob, 'structures_export.pdf'));
  }

  private downloadFile(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    this.notify.success(`Fichier ${filename} généré`);
  }
}
