/** Explicitly distinguish missing information from a zero result. */
export function statusLabel(status:string):string {
  switch(status) {
    case 'AVAILABLE': return 'Data available.';
    case 'REFERENCE_ONLY': return 'Reference geography only. Not verified for legal 2026 district use.';
    case 'AVAILABLE_DEMO': return 'Fictional fixtures — no live election feed is connected.';
    case 'NOT_IMPORTED': return 'Historical results have not been imported.';
    case 'NOT_LICENSED': return 'Source rights have not been approved for publication.';
    case 'PARTIAL': return 'Coverage is incomplete for this selection.';
    case 'LOADING': return 'Loading source data…';
    case 'NOT_CONNECTED': return 'No licensed provider is connected.';
    case 'NOT_PUBLISHED': return 'No authorized exit survey is available.';
    case 'NOT_ON_BALLOT': return 'Not on the ballot for this election.';
    case 'NOT_REPORTING': return 'This race has not started reporting.';
    case 'STALE': return 'The latest source data are stale.';
    case 'UNAVAILABLE_NOT_TRAINED': return 'Model unavailable: no validated model has been trained.';
    case 'ERROR': return 'Data unavailable. Check the local backend.';
    default: return status;
  }
}
