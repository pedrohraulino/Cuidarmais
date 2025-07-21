package cuidar.mais.api.service;

import cuidar.mais.api.dto.PacienteDTO;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Paciente;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.PacienteRepository;
import cuidar.mais.api.repository.HorarioDisponivelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PacienteService {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private HorarioDisponivelService horarioDisponivelService;

    @Autowired
    private HorarioDisponivelRepository horarioDisponivelRepository;

    @Autowired
    private SessaoService sessaoService;

    /**
     * Busca todos os pacientes ativos de um psicólogo
     */
    public List<PacienteDTO> buscarPorPsicologo(Long psicologoId) {
        List<Paciente> pacientes = pacienteRepository.findByPsicologoIdAndAtivoTrueOrderByNome(psicologoId);
        return pacientes.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca todos os pacientes (incluindo inativos) de um psicólogo
     */
    public List<PacienteDTO> buscarTodosPorPsicologo(Long psicologoId) {
        List<Paciente> pacientes = pacienteRepository.findByPsicologoIdOrderByNome(psicologoId);
        return pacientes.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca um paciente por ID
     */
    public PacienteDTO buscarPorId(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        return converterParaDTO(paciente);
    }

    /**
     * Cadastra um novo paciente
     */
    @Transactional
    public PacienteDTO cadastrar(PacienteDTO dto) {
        Usuario psicologo = usuarioService.buscarPorId(dto.getPsicologoId());

        // Verifica se o email já existe para este psicólogo
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (pacienteRepository.existsByEmailAndPsicologo(dto.getEmail(), psicologo)) {
                throw new RuntimeException("Já existe um paciente com este email para este psicólogo");
            }
        }

        Paciente paciente = new Paciente();
        preencherDadosPaciente(paciente, dto);
        paciente.setPsicologo(psicologo);

        paciente = pacienteRepository.save(paciente);

        // Se foi definido um horário, vincula o paciente ao horário e cria as sessões
        if (paciente.temHorarioDefinido()) {
            vincularHorarioECriarSessoes(paciente);
        }

        return converterParaDTO(paciente);
    }

    /**
     * Atualiza um paciente existente
     */
    @Transactional
    public PacienteDTO atualizar(Long id, PacienteDTO dto) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Verifica email duplicado (exceto o próprio paciente)
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (!dto.getEmail().equals(paciente.getEmail()) &&
                pacienteRepository.existsByEmailAndPsicologo(dto.getEmail(), paciente.getPsicologo())) {
                throw new RuntimeException("Já existe um paciente com este email para este psicólogo");
            }
        }

        // Salva dados antigos
        Long horarioAnterior = paciente.getHorarioDisponivelId();
        Integer sessoesPorPacoteAnterior = paciente.getSessoesPorPacote();

        preencherDadosPaciente(paciente, dto);
        paciente = pacienteRepository.save(paciente);

        // Verifica se o horário mudou
        boolean horarioMudou = !java.util.Objects.equals(horarioAnterior, paciente.getHorarioDisponivelId());

        // Verifica se o número de sessões por pacote mudou
        boolean sessoesPorPacoteMudou = !java.util.Objects.equals(sessoesPorPacoteAnterior, paciente.getSessoesPorPacote());

        if (horarioMudou) {
            // Desvincula do horário anterior
            if (horarioAnterior != null) {
                horarioDisponivelService.desvincularPaciente(paciente.getId());
            }

            // Cancela todas as sessões futuras
            sessaoService.cancelarSessoesFuturas(paciente);

            // Vincula ao novo horário se definido
            if (paciente.getHorarioDisponivelId() != null) {
                vincularHorarioECriarSessoes(paciente);
            }
        } else if (sessoesPorPacoteMudou && paciente.temHorarioDefinido()) {
            // Se apenas o número de sessões mudou, recria sessões
            sessaoService.cancelarSessoesFuturas(paciente);
            sessaoService.criarSessoesParaPaciente(paciente);
        }

        return converterParaDTO(paciente);
    }

    /**
     * Inativa um paciente
     */
    @Transactional
    public void inativar(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        paciente.setAtivo(false);
        
        // Remove a vinculação do horário
        if (paciente.getHorarioDisponivelId() != null) {
            horarioDisponivelService.desvincularPaciente(paciente.getId());
            paciente.setHorarioDisponivelId(null);
        }
        
        pacienteRepository.save(paciente);

        // Cancela todas as sessões futuras
        sessaoService.cancelarSessoesFuturas(paciente);
    }

    /**
     * Reativa um paciente
     */
    @Transactional
    public PacienteDTO reativar(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        paciente.setAtivo(true);
        paciente = pacienteRepository.save(paciente);

        return converterParaDTO(paciente);
    }

    /**
     * Adiciona um horário a um paciente e cria novas sessões
     * Agora recebe o ID do horário disponível ao invés de dia/hora
     */
    @Transactional
    public PacienteDTO adicionarHorario(Long pacienteId, Long horarioDisponivelId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        if (!paciente.getAtivo()) {
            throw new RuntimeException("Não é possível adicionar horário a um paciente inativo");
        }

        // Desvincula do horário anterior se houver
        if (paciente.getHorarioDisponivelId() != null) {
            horarioDisponivelService.desvincularPaciente(paciente.getId());
            sessaoService.cancelarSessoesFuturas(paciente);
        }

        // Define o novo horário
        paciente.setHorarioDisponivelId(horarioDisponivelId);
        paciente = pacienteRepository.save(paciente);

        // Vincula ao novo horário e cria sessões
        vincularHorarioECriarSessoes(paciente);

        return converterParaDTO(paciente);
    }

    /**
     * Gera mais sessões para um paciente
     */
    @Transactional
    public void gerarMaisSessoes(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        if (!paciente.getAtivo()) {
            throw new RuntimeException("Não é possível gerar sessões para um paciente inativo");
        }

        if (!paciente.temHorarioDefinido()) {
            throw new RuntimeException("Paciente não possui horário definido");
        }

        sessaoService.criarSessoesParaPaciente(paciente);
    }

    /**
     * Cria sessões adicionais para um paciente
     * Só permite criar se o horário pertencer ao paciente e estiver ocupado
     */
    @Transactional
    public void criarSessoesAdicionais(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        
        if (!paciente.getAtivo()) {
            throw new RuntimeException("Não é possível criar sessões para paciente inativo");
        }
        
        if (!paciente.temHorarioDefinido()) {
            throw new RuntimeException("Paciente não possui horário definido");
        }
        
        // Verifica se existe um horário disponível vinculado a este paciente
        Optional<HorarioDisponivel> horarioDisponivel = horarioDisponivelService
                .buscarHorarioPorPaciente(paciente.getId());
        
        if (horarioDisponivel.isEmpty()) {
            throw new RuntimeException("Nenhum horário encontrado para este paciente");
        }
        
        HorarioDisponivel horario = horarioDisponivel.get();
        
        // Verifica se o horário realmente pertence ao paciente
        if (!paciente.getId().equals(horario.getPacienteId())) {
            throw new RuntimeException("O horário não pertence a este paciente");
        }
        
        // Cria as sessões adicionais
        sessaoService.criarSessoesParaPaciente(paciente);
    }

    /**
     * Vincula paciente ao horário disponível e cria sessões
     */
    private void vincularHorarioECriarSessoes(Paciente paciente) {
        if (paciente.getHorarioDisponivelId() == null) {
            throw new RuntimeException("Paciente não possui horário definido");
        }

        try {
            // Busca o horário pelo ID fornecido
            Optional<HorarioDisponivel> horarioOpt = horarioDisponivelRepository.findById(paciente.getHorarioDisponivelId());
            
            if (horarioOpt.isEmpty()) {
                throw new RuntimeException("Horário não encontrado com ID: " + paciente.getHorarioDisponivelId());
            }
            
            HorarioDisponivel horario = horarioOpt.get();
            
            // Verifica se o horário está disponível (não ocupado por outro paciente)
            if (horario.getPacienteId() != null && !horario.getPacienteId().equals(paciente.getId())) {
                throw new RuntimeException("Horário já está ocupado por outro paciente");
            }
            
            // Vincula o paciente ao horário
            horario.vincularPaciente(paciente.getId());
            horarioDisponivelRepository.save(horario);

            // Cria as sessões
            sessaoService.criarSessoesParaPaciente(paciente);

        } catch (RuntimeException e) {
            throw new RuntimeException("Erro ao vincular horário: " + e.getMessage());
        }
    }

    /**
     * Preenche os dados do paciente com base no DTO
     */
    private void preencherDadosPaciente(Paciente paciente, PacienteDTO dto) {
        paciente.setNome(dto.getNome());
        paciente.setSobrenome(dto.getSobrenome());
        paciente.setSexo(dto.getSexo());
        paciente.setDataNascimento(dto.getDataNascimento());
        paciente.setEmail(dto.getEmail());
        paciente.setTelefone(dto.getTelefone());
        paciente.setImagemBase64(dto.getImagemBase64());
        paciente.setImagemTipo(dto.getImagemTipo());
        paciente.setHorarioDisponivelId(dto.getHorarioDisponivelId());
        paciente.setSessoesPorPacote(dto.getSessoesPorPacote());
        
        if (dto.getAtivo() != null) {
            paciente.setAtivo(dto.getAtivo());
        }
    }

    /**
     * Converte entidade para DTO
     */
    private PacienteDTO converterParaDTO(Paciente paciente) {
        PacienteDTO dto = new PacienteDTO();
        dto.setId(paciente.getId());
        dto.setNome(paciente.getNome());
        dto.setSobrenome(paciente.getSobrenome());
        dto.setSexo(paciente.getSexo());
        dto.setDataNascimento(paciente.getDataNascimento());
        dto.setEmail(paciente.getEmail());
        dto.setTelefone(paciente.getTelefone());
        dto.setImagemBase64(paciente.getImagemBase64());
        dto.setImagemTipo(paciente.getImagemTipo());
        dto.setPsicologoId(paciente.getPsicologo().getId());
        dto.setHorarioDisponivelId(paciente.getHorarioDisponivelId());
        dto.setSessoesPorPacote(paciente.getSessoesPorPacote());
        dto.setDataCriacao(paciente.getDataCriacao());
        dto.setAtivo(paciente.getAtivo());

        // Campos auxiliares
        dto.setNomeCompleto(paciente.getNomeCompleto());
        dto.setImagemDataUrl(paciente.getImagemDataUrl());

        // Busca informações do horário se vinculado
        if (paciente.getHorarioDisponivelId() != null) {
            Optional<HorarioDisponivel> horarioOpt = horarioDisponivelService
                    .buscarHorarioPorPaciente(paciente.getId());
            if (horarioOpt.isPresent()) {
                HorarioDisponivel horario = horarioOpt.get();
                dto.setDiaSemana(horario.getDiaSemana());
                dto.setHorarioInicio(horario.getHoraInicio());
                dto.setHorarioFim(horario.getHoraFim());
                dto.setNomeDiaSemana(getNomeDiaSemana(horario.getDiaSemana()));
                dto.setHorarioFormatado(horario.getHoraInicio() + " - " + horario.getHoraFim());
            }
        }

        // Estatísticas de sessões (se necessário)
        if (paciente.getId() != null) {
            long realizadas = sessaoService.contarSessoesRealizadas(paciente.getId());
            long agendadas = sessaoService.contarSessoesAgendadas(paciente);
            
            dto.setSessoesRealizadas((int) realizadas);
            dto.setSessoesRestantes((int) agendadas);
        }

        return dto;
    }

    private String getNomeDiaSemana(DayOfWeek diaSemana) {
        if (diaSemana == null) return "Não definido";
        
        return switch (diaSemana) {
            case MONDAY -> "Segunda-feira";
            case TUESDAY -> "Terça-feira";
            case WEDNESDAY -> "Quarta-feira";
            case THURSDAY -> "Quinta-feira";
            case FRIDAY -> "Sexta-feira";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }
}
