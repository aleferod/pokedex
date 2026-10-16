import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useSearchParams } from 'react-router';

import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';
import {
  fieldMessages,
  FormAlert,
  showServerErrors,
  SubmitButton,
  TextField,
} from '@/shared/ui/form';

import { safeNext } from './safeNext';
import { registerSchema, type RegisterValues } from './schemas';
import { useAuth } from './useAuth';

export function RegisterPage() {
  useDocumentTitle('Register');
  const { session, register: registerAccount } = useAuth();
  const [searchParams] = useSearchParams();
  const next = safeNext(searchParams.get('next'));
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched',
    criteriaMode: 'all',
    defaultValues: { username: '', email: '', password: '' },
  });

  // Registering signs the account in, which lands here with a session.
  if (session !== null) {
    return <Navigate to={next} replace />;
  }

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await registerAccount(values);
    } catch (error) {
      setFormError(showServerErrors(error, ['username', 'email', 'password'], setError));
    }
  });

  const signInLink = next === '/' ? '/login' : `/login?next=${encodeURIComponent(next)}`;

  return (
    <section aria-labelledby="register-title" className="mx-auto max-w-md space-y-6 py-8">
      <h1 id="register-title" className="text-2xl font-bold">
        Create an account
      </h1>
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
          hint="3 to 30 characters: letters, digits, '.', '_' or '-'."
          errors={fieldMessages(errors.username)}
          {...register('username')}
        />
        <TextField
          id="email"
          label="Email"
          type="email"
          autoComplete="email"
          errors={fieldMessages(errors.email)}
          {...register('email')}
        />
        <TextField
          id="password"
          label="Password"
          type="password"
          autoComplete="new-password"
          hint="8 to 72 characters, with an upper-case letter, a lower-case letter, a digit and a symbol."
          errors={fieldMessages(errors.password)}
          {...register('password')}
        />
        <SubmitButton busy={isSubmitting} busyLabel="Creating account…">
          Create account
        </SubmitButton>
      </form>
      <p className="text-slate-700">
        Already registered?{' '}
        <Link to={signInLink} className="font-semibold text-red-700 underline">
          Sign in instead
        </Link>
      </p>
    </section>
  );
}
