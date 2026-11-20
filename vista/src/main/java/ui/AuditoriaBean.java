package ui;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import facade.SistemaFacade;
import mx.puntodeventa.entity.MovimientoInventario;
import mx.puntodeventa.entity.Usuario;

@Named("AuditoriaBean")
@ViewScoped
public class AuditoriaBean implements Serializable {

    private List<MovimientoInventario> listaMovimientos;
    private List<MovimientoInventario> listaFiltrada;
    private List<Usuario> listaUsuarios;

    private Integer idUsuarioFiltro;
    private String productoFiltro;
    private Integer idMovimientoFiltro;
    private String tipoFiltro;

    private Date fechaDesde;
    private Date fechaHasta;
    private boolean ordenAscendente = false;

    private SistemaFacade facade;

    private int totalEntradas;
    private int totalSalidas;

    private MovimientoInventario movimientoSeleccionado;

    private Usuario usuarioLogeado;
    LoginBeanUI loginBeanUI;

    @PostConstruct
    public void inicio() {
        loginBeanUI = new LoginBeanUI();
        facade = new SistemaFacade();

        listaMovimientos = new ArrayList<>();
        listaFiltrada = new ArrayList<>();
        listaUsuarios = new ArrayList<>();

        cargarUsuarios();
        cargarTodosLosMovimientos();
    }

    public void cargarTodosLosMovimientos() {
        try {
            listaMovimientos = facade.listarTodosLosMovimientos();
            aplicarFiltros();
        } catch (Exception e) {
            e.printStackTrace();
            listaMovimientos = new ArrayList<>();
            listaFiltrada = new ArrayList<>();
        }
    }

    public void cargarUsuarios() {
        try {
            listaUsuarios = facade.listarUsuarios();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void aplicarFiltros() {

        listaFiltrada = new ArrayList<>();

        totalEntradas = 0;
        totalSalidas = 0;

        if (listaMovimientos == null) {
            return;
        }

        for (MovimientoInventario mov : listaMovimientos) {

            if (mov == null) {
                continue;
            }


            boolean pasaFiltroId = idMovimientoFiltro == null || idMovimientoFiltro <= 0 || mov.getId() == idMovimientoFiltro;
            boolean pasaFiltroUsuario = idUsuarioFiltro == null || idUsuarioFiltro == 0 || (mov.getUsuario() != null && mov.getUsuario().getId() == idUsuarioFiltro);
            boolean pasaFiltroProducto = true;

            if (productoFiltro != null && !productoFiltro.trim().isEmpty()) {
                String filtro = productoFiltro.trim().toLowerCase();
                if (mov.getProducto() == null) {
                    pasaFiltroProducto = false;
                } else {
                    String nombreProducto = mov.getProducto().getNombre() != null ? mov.getProducto().getNombre().toLowerCase() : "";
                    String identificadorProducto = mov.getProducto().getIdentificador() != null ? mov.getProducto().getIdentificador().toLowerCase() : "";
                    String idProducto =
                            String.valueOf(mov.getProducto().getId());
                    pasaFiltroProducto =
                            nombreProducto.contains(filtro)
                                    || identificadorProducto.contains(filtro)
                                    || idProducto.contains(filtro);
                }
            }
            boolean pasaFiltroTipo = tipoFiltro == null || tipoFiltro.trim().isEmpty() || mov.getTipo().equalsIgnoreCase(tipoFiltro);
            boolean pasaFiltroFecha = true;

            if (fechaDesde != null || fechaHasta != null) {
                Date fechaMov = mov.getFecha();
                if (fechaMov == null) {
                    pasaFiltroFecha = false;
                } else {
                    if (fechaDesde != null) {
                        Calendar calDesde = Calendar.getInstance();
                        calDesde.setTime(fechaDesde);
                        calDesde.set(Calendar.HOUR_OF_DAY, 0);
                        calDesde.set(Calendar.MINUTE, 0);
                        calDesde.set(Calendar.SECOND, 0);
                        calDesde.set(Calendar.MILLISECOND, 0);
                        if (fechaMov.before(calDesde.getTime())) {
                            pasaFiltroFecha = false;
                        }
                    }
                    if (fechaHasta != null && pasaFiltroFecha) {
                        Calendar calHasta = Calendar.getInstance();
                        calHasta.setTime(fechaHasta);
                        calHasta.set(Calendar.HOUR_OF_DAY, 23);
                        calHasta.set(Calendar.MINUTE, 59);
                        calHasta.set(Calendar.SECOND, 59);
                        calHasta.set(Calendar.MILLISECOND, 999);
                        if (fechaMov.after(calHasta.getTime())) {
                            pasaFiltroFecha = false;
                        }
                    }
                }
            }
            if (pasaFiltroId && pasaFiltroUsuario && pasaFiltroProducto && pasaFiltroTipo && pasaFiltroFecha) {
                listaFiltrada.add(mov);
                if ("Entrada".equalsIgnoreCase(mov.getTipo())) {
                    totalEntradas += mov.getCantidad();
                } else if ("Salida".equalsIgnoreCase(mov.getTipo())) {
                    totalSalidas += mov.getCantidad();
                }
            }
        }
        listaFiltrada.sort((m1, m2) -> {

            if (m1.getFecha() == null || m2.getFecha() == null) {
                return 0;
            }
            return ordenAscendente ? m1.getFecha().compareTo(m2.getFecha()) : m2.getFecha().compareTo(m1.getFecha());
        });
    }

    public void alternarOrden() {
        this.ordenAscendente = !this.ordenAscendente;
        aplicarFiltros();
    }

    public double calcularValorTotal(MovimientoInventario mov) {
        if (mov == null || mov.getProducto() == null) {
            return 0;
        }
        System.out.println(mov.getProducto().getNombre()+ " \n" + "Cantidad: " + mov.getCantidad() + "\n" + "Precio unitario: "  + mov.getProducto().getPrecio());
        return mov.getCantidad() * mov.getProducto().getPrecio();

    }

    public void seleccionarMovimiento(MovimientoInventario mov) {
        this.movimientoSeleccionado = mov;
    }

    public void limpiarFiltros() {

        idUsuarioFiltro = null;
        productoFiltro = null;
        idMovimientoFiltro = null;
        tipoFiltro = null;

        fechaDesde = null;
        fechaHasta = null;

        aplicarFiltros();
    }

    public boolean puedeVerAuditoria(Usuario usuarioLogeado) {
        return usuarioLogeado != null;
    }

    public List<MovimientoInventario> getListaMovimientos() {
        return listaMovimientos;
    }

    public void setListaMovimientos(List<MovimientoInventario> listaMovimientos) {
        this.listaMovimientos = listaMovimientos;
    }

    public List<MovimientoInventario> getListaFiltrada() {
        return listaFiltrada;
    }

    public Integer getIdMovimientoFiltro() {
        return idMovimientoFiltro;
    }

    public void setIdMovimientoFiltro(Integer idMovimientoFiltro) {
        this.idMovimientoFiltro = idMovimientoFiltro;
    }

    public String getTipoFiltro() {
        return tipoFiltro;
    }

    public void setTipoFiltro(String tipoFiltro) {
        this.tipoFiltro = tipoFiltro;
    }


    public void setListaFiltrada(List<MovimientoInventario> listaFiltrada) {
        this.listaFiltrada = listaFiltrada;
    }

    public List<Usuario> getListaUsuarios() {
        return listaUsuarios;
    }

    public void setListaUsuarios(List<Usuario> listaUsuarios) {
        this.listaUsuarios = listaUsuarios;
    }

    public Integer getIdUsuarioFiltro() {
        return idUsuarioFiltro;
    }

    public void setIdUsuarioFiltro(Integer idUsuarioFiltro) {
        this.idUsuarioFiltro = idUsuarioFiltro;
    }

    public String getProductoFiltro() {
        return productoFiltro;
    }

    public void setProductoFiltro(String productoFiltro) {
        this.productoFiltro = productoFiltro;
    }

    public Date getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(Date fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public Date getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(Date fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public boolean isOrdenAscendente() {
        return ordenAscendente;
    }

    public void setOrdenAscendente(boolean ordenAscendente) {
        this.ordenAscendente = ordenAscendente;
    }

    public int getTotalEntradas() {
        return totalEntradas;
    }

    public int getTotalSalidas() {
        return totalSalidas;
    }

    public MovimientoInventario getMovimientoSeleccionado() {
        return movimientoSeleccionado;
    }

    public void setMovimientoSeleccionado(MovimientoInventario movimientoSeleccionado) {
        this.movimientoSeleccionado = movimientoSeleccionado;
    }
}