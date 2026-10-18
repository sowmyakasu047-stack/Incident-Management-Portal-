import Navbar from './Navbar';

export default function Layout({ children }) {
  return (
    <>
      <Navbar />
      <main className="container pb-5">{children}</main>
    </>
  );
}
