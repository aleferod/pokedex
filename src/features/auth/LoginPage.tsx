import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useSearchParams } from 'react-router';

import { ApiError } from '@/shared/api';
import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';
import { fieldMessages, FormAlert, SubmitButton, TextField } from '@/shared/ui/form';

import { safeNext } from './safeNext';
import { signInSchema, type SignInValues } from './schemas';
import { showServerErrors } from './serverErrors';
import { useAuth } from './useAuth';

const WRONG_CREDENTIALS = 'Invalid username or password.';

export function LoginPage() {
  useDocumentTitle('Sign in');
  const { session, notice, signIn } = useAuth();
  const [searchParams] = useSearchParams();
  const next = safeNext(searchParams.get('next'));
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<SignInValues>({
    resolver: zodResolver(signInSchema),
    mode: 'onTouched',
    criteriaMode: 'all',
    defaultValues: { username: '', password: '' },
  });

  if (session !== null) {
    return <Navigate to={next} replace />;
  }

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await signIn(values);
    } catch (error) {
      // Never say whether the username or the password was wrong.
      if (error instanceof ApiError && error.status === 401) {
        setFormError(WRONG_CREDENTIALS);
        return;
      }
      setFormError(showServerErrors(error, ['username', 'password'], setError));
    }
  });

  const registerLink = next === '/' ? '/register' : `/register?next=${encodeURIComponent(next)}`;

  return (
    <section aria-labelledby="sign-in-title" className="mx-auto max-w-md space-y-6 py-8">
      <h1 id="sign-in-title" className="text-2xl font-bold">
        Sign in
      </h1>
      {notice === 'expired' && (
        <p role="status" className="rounded-md bg-amber-100 px-3 py-2 text-amber-900">
          Your session has expired. Please sign in again.
        </p>
      )}
      {formError !== null && <FormAlert>{formError}</FormAlert>}
      <form
        noValidate
        onSubmit={(event) => {
          void onSubmit(event);
        }}
        className="space-y-4"
      >
        <TextField
          id="username"
          label="Username"
          autoComplete="username"
          errors={fieldMessages(errors.username)}
          {...register('username')}
        />
        <TextField
          id="password"
          label="Password"
          type="password"
          autoComplete="current-password"
          errors={fieldMessages(errors.password)}
          {...register('password')}
        />
        <SubmitButton busy={isSubmitting} busyLabel="Signing in…">
          Sign in
        </SubmitButton>
      </form>
      <p className="text-slate-700">
        No account yet?{' '}
        <Link to={registerLink} className="font-semibold text-red-700 underline">
          Create an account
        </Link>
      </p>
    </section>
  );
}
