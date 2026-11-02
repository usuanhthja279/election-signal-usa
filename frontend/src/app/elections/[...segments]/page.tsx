import ElectionShell from '../../../components/ElectionShell';

/** Query-backed election context lives in the shell; this route enables semantic deep links. */
export default function ElectionDeepLinkPage() {
  return <ElectionShell />;
}
