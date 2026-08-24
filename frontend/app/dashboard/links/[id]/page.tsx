import LinkDetailClient from "./LinkDetailClient";

export default async function LinkDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <LinkDetailClient linkId={id} />;
}
