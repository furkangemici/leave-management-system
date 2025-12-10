import { createContext, useState, useEffect } from "react";
import { axiosPrivate } from "../api/axios";

const AuthContext = createContext({});

export const AuthProvider = ({ children }) => {
    const [auth, setAuth] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check local storage on load
        const storedUser = localStorage.getItem("user");
        const storedToken = localStorage.getItem("token");

        if (storedUser && storedToken) {
            setAuth({
                user: JSON.parse(storedUser),
                token: storedToken
            });
            // Optionally: Validate token with backend here
        }
        setLoading(false);
    }, []);

    const login = (userData, token, rememberMe = false) => {
        setAuth({ user: userData, token });
        if (rememberMe) {
            localStorage.setItem("user", JSON.stringify(userData));
            localStorage.setItem("token", token);
        } else {
             // If not remember me, we might still want to store it in session or just memory
             // For this simple implementation, we'll store in localStorage but maybe clear on close (sessionStorage)
             // However, the request asked specifically for "Remember Me" logic.
             // Usually "Remember Me" means persist across browser restarts.
             // If unchecked, we might use sessionStorage instead.
             sessionStorage.setItem("user", JSON.stringify(userData));
             sessionStorage.setItem("token", token);
        }
    };

    const logout = () => {
        setAuth(null);
        localStorage.removeItem("user");
        localStorage.removeItem("token");
        sessionStorage.removeItem("user");
        sessionStorage.removeItem("token");
    };

    return (
        <AuthContext.Provider value={{ auth, setAuth: login, logout, isLoading: loading }}>
             {!loading && children}
        </AuthContext.Provider>
    );
};

export default AuthContext;
