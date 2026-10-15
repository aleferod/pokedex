import { Component, type ReactNode } from 'react';

import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  failed: boolean;
}

/**
 * Catches an error thrown while rendering its children and shows a friendly fallback with a
 * "Try again" button that renders them again. The error itself is never shown: React reports it
 * on the console for developers.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { failed: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { failed: true };
  }

  private readonly retry = (): void => {
    this.setState({ failed: false });
  };

  override render(): ReactNode {
    if (this.state.failed) {
      return <ErrorFallback onRetry={this.retry} />;
    }
    return this.props.children;
  }
}

function ErrorFallback({ onRetry }: { onRetry: () => void }) {
  useDocumentTitle('Something went wrong');
  return (
    <section aria-labelledby="error-title" className="space-y-4 py-12 text-center">
      <h1 id="error-title" className="text-2xl font-bold">
        Something went wrong
      </h1>
      <p className="text-slate-600">An unexpected error stopped this page from showing.</p>
      <button
        type="button"
        onClick={onRetry}
        className="rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700"
      >
        Try again
      </button>
    </section>
  );
}
